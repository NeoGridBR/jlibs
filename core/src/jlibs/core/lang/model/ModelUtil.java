/*
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.core.lang.model;

import jlibs.core.annotation.processing.AnnotationError;
import jlibs.core.annotation.processing.Environment;
import jlibs.core.lang.BeanUtil;
import jlibs.core.lang.NotImplementedException;
import jlibs.core.util.regex.TemplateMatcher;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Santhosh Kumar T
 */
public class ModelUtil{
    @SuppressWarnings({"unchecked"})
    public static <T> T parent(Element element, Class<T> type){
        do{
            element = element.getEnclosingElement();
        }while(!type.isInstance(element));
        return (T)element;
    }

    public static TypeElement getSuper(TypeElement clazz){
        TypeMirror superMirror = clazz.getSuperclass();
        if(superMirror instanceof DeclaredType)
            return (TypeElement)((DeclaredType)superMirror).asElement();
        else
            return null;
    }

    public static String getPackage(TypeElement clazz){
        return ((PackageElement)clazz.getEnclosingElement()).getQualifiedName().toString();
    }

    public static String toString(TypeMirror mirror){
        switch(mirror.getKind()){
            case DECLARED:
                Name paramType = ((TypeElement)((DeclaredType)mirror).asElement()).getQualifiedName();
                return paramType.toString();
            case INT:
                return "java.lang.Integer";
            case BOOLEAN:
            case FLOAT:
            case DOUBLE:
            case LONG:
            case BYTE:
                return "java.lang."+ BeanUtil.firstLetterToUpperCase(mirror.getKind().toString().toLowerCase());
            case ARRAY:
                return toString(((ArrayType)mirror).getComponentType())+"[]";
            default:
                throw new NotImplementedException(mirror.getKind()+" is not implemented for "+mirror.getClass());
        }
    }

    public static String signature(ExecutableElement method, boolean useParamNames){
        StringBuilder buff = new StringBuilder();

        buff.append(toString(method.getReturnType()));
        buff.append(' ');
        buff.append(method.getSimpleName());
        buff.append('(');
        int i = 0;
        for(VariableElement param : method.getParameters()){
            if(i>0)
                buff.append(", ");
            buff.append(toString(param.asType()));
            if(useParamNames)
                buff.append(' ').append(param.getSimpleName());
            i++;
        }
        buff.append(')');

        return buff.toString();
    }

    /*-------------------------------------------------[ Annotation ]---------------------------------------------------*/

    public static boolean matches(AnnotationMirror mirror, Class annotation){
        return ((TypeElement)mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(annotation.getCanonicalName());
    }

    public static AnnotationMirror getAnnotationMirror(Element elem, Class annotation){
        for(AnnotationMirror mirror: elem.getAnnotationMirrors()){
            if(matches(mirror, annotation))
                return mirror;
        }
        return null;
    }

    public static AnnotationValue getRawAnnotationValue(Element pos, AnnotationMirror mirror, String method){
        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()){
            if(entry.getKey().getSimpleName().contentEquals(method))
                return entry.getValue();
        }
        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : Environment.get().getElementUtils().getElementValuesWithDefaults(mirror).entrySet()){
            if(entry.getKey().getSimpleName().contentEquals(method))
                return entry.getValue();
        }
        throw new AnnotationError(pos, mirror, "annotation "+((TypeElement)mirror.getAnnotationType().asElement()).getQualifiedName()+" is missing "+method);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T getAnnotationValue(Element pos, AnnotationMirror mirror, String method){
        return (T)getRawAnnotationValue(pos, mirror, method).getValue();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(Element elem, Class annotation, String method){
        AnnotationMirror mirror = getAnnotationMirror(elem, annotation);
        if(mirror!=null)
            return (T)getAnnotationValue(elem, mirror, method);
        else
            return null;
    }


    /*-------------------------------------------------[ javadoc ]---------------------------------------------------*/

    private static final String PARAM = "@param";
    private static final String RETURN = "@return";

    public static String getMethodDoc(String doc){
        if(doc==null)
            return null;
        else{
            int index = doc.indexOf(PARAM);
            if(index==-1){
                doc = doc.trim();
            }else
                doc = doc.substring(0, index).trim();

            index = doc.indexOf(RETURN);
            if(index==-1){
                doc = doc.trim();
            }else
                doc = doc.substring(0, index).trim();
            return doc;
        }
    }

    private static String[] split(String str, boolean whitespace){
        int i = 0;
        while(i<str.length()){
            char ch = str.charAt(i);
            if(Character.isWhitespace(ch)==whitespace)
                i++;
            else
                break;
        }
        return new String[]{ str.substring(0, i), str.substring(i) };
    }

    public static Map<String, String> getMethodParamDocs(String doc){
        Map<String, String> docs = new HashMap<String, String>();
        if(doc!=null){
            int index = doc.indexOf(RETURN);
            if(index!=-1)
                doc = doc.substring(0, index);
            index = doc.indexOf(PARAM);
            if(index!=-1)
                doc = doc.substring(index).trim();
            for(String token: doc.split(PARAM)){
                token = token.trim();
                if(token.length()>0){
                    String str[] = split(token, false);
                    str[1] = str[1].trim();
                    if(str[0].length()>0 && str[1].length()>0)
                        docs.put(str[0], str[1]);
                }
            }
        }
        return docs;
    }

    /*-------------------------------------------------[ Finding Generated Class ]---------------------------------------------------*/

    public static String[] findClass(TypeElement clazz, String format){
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("package", ModelUtil.getPackage(clazz));
        vars.put("class", clazz.getSimpleName().toString());
        String qname = new TemplateMatcher("${", "}").replace(format, vars);
        String pakage, clazzName;        

        int dot = qname.lastIndexOf('.');
        if(dot==-1){
            pakage = "";
            clazzName = qname;
        }else{
            pakage = qname.substring(0, dot);
            clazzName = qname.substring(dot+1);
        }
        return new String[]{ qname, pakage, clazzName };
    }

    public static Class findClass(Class clazz, String format) throws ClassNotFoundException{
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("package", clazz.getPackage().getName());
        vars.put("class", clazz.getSimpleName());
        String qname = new TemplateMatcher("${", "}").replace(format, vars);
        return clazz.getClassLoader().loadClass(qname);
    }
}
