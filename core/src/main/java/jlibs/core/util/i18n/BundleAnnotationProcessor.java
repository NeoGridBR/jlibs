/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.core.util.i18n;

import jlibs.core.annotation.processing.AnnotationError;
import jlibs.core.annotation.processing.AnnotationProcessor;
import jlibs.core.annotation.processing.Environment;
import jlibs.core.annotation.processing.Printer;
import jlibs.core.lang.StringUtil;
import jlibs.core.lang.model.ModelUtil;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import static jlibs.core.util.i18n.PropertiesUtil.*;

/**
 * @author Santhosh Kumar T
 */
@SuppressWarnings({"unchecked"})
@SupportedAnnotationTypes({ "jlibs.core.util.i18n.ResourceBundle", "jlibs.core.util.i18n.Bundle" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions("ResourceBundle.basename")
public class BundleAnnotationProcessor extends AnnotationProcessor{
    public static final String FORMAT = "${package}._Bundle";
    private static String basename;

    private static class Info{
        private String pakage;
        private BufferedWriter props;
        private Map<String, Element> entries = new HashMap<String, Element>();
        private Interfaces interfaces;
        private Bundles bundles;

        public Info(Element element, AnnotationMirror mirror) throws IOException{
            pakage = ModelUtil.getPackage(element);
            if(ModelUtil.exists(pakage, basename+".properties"))
                throw new AnnotationError(element, mirror, basename+".properties in package "+pakage+" already exists in source path");

            FileObject resource = Environment.get().getFiler().createResource(StandardLocation.SOURCE_OUTPUT, pakage, basename+".properties");
            props = new BufferedWriter(resource.openWriter());
        }

        public void addResourceBundle(TypeElement clazz) throws IOException{
            if(interfaces==null)
                interfaces = new Interfaces(entries);
            interfaces.add(clazz);
        }

        public void addBundle(Element element) throws IOException{
            if(bundles==null)
                bundles = new Bundles();
            bundles.add(element);
        }

        public void generate() throws IOException{
            writeComments(props, " DON'T EDIT THIS FILE. THIS IS GENERATED BY JLIBS");
            writeComments(props, " @author Santhosh Kumar T");
            props.newLine();

            if(interfaces!=null){
                interfaces.generateClass(basename);
                interfaces.generateProperties(props);
            }
            if(bundles!=null)
                bundles.generateProperties(entries, props);
            
            close();
        }

        public void close() throws IOException{
            if(interfaces!=null){
                interfaces.close();
                interfaces = null;
            }
            if(props!=null){
                props.close();
                props = null;
            }
        }
    }
    private static Map<String, Info> infos = new HashMap<String, Info>();
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        basename = Environment.get().getOptions().get("ResourceBundle.basename");
        if(basename==null)
            basename = "Bundle";
        
        try{
            for(TypeElement annotation: annotations){
                if(annotation.getQualifiedName().contentEquals(ResourceBundle.class.getName())){
                    for(Element elem: roundEnv.getElementsAnnotatedWith(annotation)){
                        TypeElement c = (TypeElement)elem;
                        String pakage = ModelUtil.getPackage(c);

                        if(c.getKind()!=ElementKind.INTERFACE)
                            throw new AnnotationError(elem, ResourceBundle.class.getName()+" annotation can be applied only for interface");

                        Info info = infos.get(pakage);
                        if(info==null)
                            infos.put(pakage, info=new Info(c, ModelUtil.getAnnotationMirror(c, ResourceBundle.class)));
                        info.addResourceBundle(c);
                    }
                }else{
                    for(Element elem: roundEnv.getElementsAnnotatedWith(annotation)){
                        String pakage = ModelUtil.getPackage(elem);
                        Info info = infos.get(pakage);
                        if(info==null)
                            infos.put(pakage, info=new Info(elem, ModelUtil.getAnnotationMirror(elem, Bundle.class)));
                        info.addBundle(elem);
                    }
                }
            }

            for(Info info: infos.values())
                info.generate();
        }catch(AnnotationError error){
            error.report();
        }catch(IOException ex){
            throw new RuntimeException(ex);
        }finally{
            for(Info info : infos.values()){
                try{
                    info.close();
                }catch(IOException ignore){
                    // ignore
                }
            }
            infos.clear();
        }
        return true;
    }
}

class Interfaces{
    private List<String> interfaces = new ArrayList<String>();
    private Printer printer;
    private Map<String, ExecutableElement> entries;
    private Map<Element, Map<String, ExecutableElement>> classes = new HashMap<Element, Map<String, ExecutableElement>>();

    @SuppressWarnings({"unchecked"})
    Interfaces(Map entries){
        this.entries = entries;
    }

    public void add(TypeElement clazz) throws IOException{
        if(printer==null)
            printer = Printer.get(clazz, ResourceBundle.class, BundleAnnotationProcessor.FORMAT);

        interfaces.add(clazz.getSimpleName().toString());

        while(clazz!=null && !clazz.getQualifiedName().contentEquals(Object.class.getName())){
            for(ExecutableElement method: ElementFilter.methodsIn(clazz.getEnclosedElements()))
                add(method);
            clazz = ModelUtil.getSuper(clazz);
        }
    }
    
    private void add(ExecutableElement method){
        AnnotationMirror mirror = ModelUtil.getAnnotationMirror(method, Message.class);
        if(mirror==null)
            throw new AnnotationError(method, Message.class.getName()+" annotation is missing on this method");
        if(!String.class.getName().equals(ModelUtil.toString(method.getReturnType(), true)))
            throw new AnnotationError(method, "method annotated with "+Message.class.getName()+" must return java.lang.String");

        String signature = ModelUtil.signature(method, false);
        for(ExecutableElement m : entries.values()){
            if(signature.equals(ModelUtil.signature(m, false)))
                throw new AnnotationError(method, "clashes with similar method in "+m.getEnclosingElement()+" interface");
        }

        AnnotationMirror messageMirror = ModelUtil.getAnnotationMirror(method, Message.class);
        String key = ModelUtil.getAnnotationValue(method, messageMirror, "key");
        if(StringUtil.isEmpty(key))
            key = method.getSimpleName().toString();

        ExecutableElement clash = entries.put(key, method);
        Element interfase = method.getEnclosingElement();
        if(clash!=null)
            throw new AnnotationError(method, "key '"+key+"' is already used by \""+ModelUtil.signature(clash, false)+"\" in "+ clash.getEnclosingElement()+" interface");

        Map<String, ExecutableElement> methods = classes.get(interfase);
        if(methods==null)
            classes.put(interfase, methods=new HashMap<String, ExecutableElement>());
        methods.put(key, method);
    }

    public void generateClass(String basename) throws IOException{
        printer.printPackage();

        printer.println("import java.util.ResourceBundle;");
        printer.println("import java.text.MessageFormat;");
        printer.emptyLine(true);

        printer.printClassDoc();
        printer.println("@SuppressWarnings(\"unchecked\")");
        printer.println("public class "+printer.generatedClazz +" implements "+StringUtil.join(interfaces.iterator(), ", ")+"{");
        printer.indent++;

        printer.println("public static final "+printer.generatedClazz +" INSTANCE = new "+printer.generatedClazz +"();");
        printer.emptyLine(true);
        printer.println("private final ResourceBundle BUNDLE = ResourceBundle.getBundle(\""+printer.generatedPakage.replace('.', '/')+"/"+basename+"\");");
        printer.emptyLine(true);

        for(Map.Entry<Element, Map<String, ExecutableElement>> methods : classes.entrySet()){
            printer.emptyLine(true);
            printer.println("/*-------------------------------------------------[ "+methods.getKey().getSimpleName()+" ]---------------------------------------------------*/");
            printer.emptyLine(true);

            for(Map.Entry<String, ExecutableElement> entry : methods.getValue().entrySet()){
                String key = entry.getKey();
                ExecutableElement method = entry.getValue();

                printer.println("@Override");
                printer.print("public String "+method.getSimpleName()+"(");

                int i = 0;
                StringBuilder params = new StringBuilder();
                for(VariableElement param : method.getParameters()){
                    String paramName = param.getSimpleName().toString();

                    params.append(", ");
                    if(i>0)
                        printer.print(", ");
                    params.append(paramName);
                    printer.print(ModelUtil.toString(param.asType(), false)+" "+paramName);
                    i++;
                }
                if(params.length()==0)
                    params.append(", new Object[0]");

                printer.println("){");
                printer.indent++;
                printer.println("return MessageFormat.format(BUNDLE.getString(\""+key+"\")"+params+");");
                printer.indent--;
                printer.println("}");
            }
        }

        printer.indent--;
        printer.println("}");

        close();
    }

    public void generateProperties(BufferedWriter props) throws IOException{
        Elements elemUtil = Environment.get().getElementUtils();
        for(Map.Entry<Element, Map<String, ExecutableElement>> methods : classes.entrySet()){
            writeComments(props, "-------------------------------------------------[ "+methods.getKey().getSimpleName()+" ]---------------------------------------------------");
            props.newLine();

            for(Map.Entry<String, ExecutableElement> entry : methods.getValue().entrySet()){
                String key = entry.getKey();
                ExecutableElement method = entry.getValue();

                String doc = elemUtil.getDocComment(method);
                String methodDoc = ModelUtil.getMethodDoc(doc);
                if(!StringUtil.isEmpty(methodDoc))
                    writeComments(props, " "+methodDoc);

                int i = 0;
                Map<String, String> paramDocs = ModelUtil.getMethodParamDocs(doc);
                for(VariableElement param : method.getParameters()){
                    String paramName = param.getSimpleName().toString();
                    String paramDoc = paramDocs.get(paramName);
                    if(StringUtil.isEmpty(paramDoc))
                        writeComments(props, " {"+i+"} "+paramName);
                    else
                        writeComments(props, " {"+i+"} "+paramName+" ==> "+paramDoc);
                    i++;
                }

                AnnotationMirror messageMirror = ModelUtil.getAnnotationMirror(method, Message.class);
                String value = ModelUtil.getAnnotationValue(method, messageMirror, "value");

                try{
                    new MessageFormat(value);
                }catch(IllegalArgumentException ex){
                    throw new AnnotationError(method, messageMirror, ModelUtil.getRawAnnotationValue(method, messageMirror, "value"), "Invalid Message Format: "+ex.getMessage());
                }

                NavigableSet<Integer> args = findArgs(value);
                int argCount = args.size()==0 ? 0 : (args.last()+1);
                if(argCount!=method.getParameters().size())
                    throw new AnnotationError(method, "no of args in message format doesn't match with the number of parameters this method accepts");
                for(i=0; i<argCount; i++){
                    if(!args.remove(i))
                        throw new AnnotationError(method, messageMirror, "{"+i+"} is missing in message");
                }

                writeProperty(props, key, value);
                props.newLine();
            }
        }
    }

    public void close() throws IOException{
        if(printer!=null){
            printer.close();
            printer = null;
        }
    }
}

class Bundles{
    private Map<Element, List<Element>> classes = new HashMap<Element, List<Element>>();

    public void add(Element element){
        Element container = element.getEnclosingElement();
        if(container instanceof PackageElement)
            container = element;
        List<Element> list = classes.get(container);
        if(list==null)
            classes.put(container, list=new ArrayList<Element>());
        list.add(element);
    }

    @SuppressWarnings({"unchecked"})
    public void generateProperties(Map<String, Element> entries, BufferedWriter props) throws IOException{
        for(Map.Entry<Element, List<Element>> entry: classes.entrySet()){
            writeComments(props, "-------------------------------------------------[ "+entry.getKey().getSimpleName()+" ]---------------------------------------------------");
            props.newLine();
            
            for(Element method : entry.getValue()){
                AnnotationMirror mirror = ModelUtil.getAnnotationMirror(method, Bundle.class);
                for(AnnotationValue value: (Collection<AnnotationValue>)ModelUtil.getAnnotationValue(method, mirror, "value")){
                    AnnotationMirror entryMirror = (AnnotationMirror)value.getValue();
                    String rhs = ModelUtil.getAnnotationValue(method, entryMirror, "rhs");
                    if(rhs.length()>0){
                        String lhs = ModelUtil.getAnnotationValue(method, entryMirror, "lhs");
                        if(lhs.length()==0){
                            String hintName = ModelUtil.getAnnotationValue(method, entryMirror, "hintName");
                            if(hintName.length()==0){
                                Hint hint = Hint.valueOf(((VariableElement)ModelUtil.getAnnotationValue(method, entryMirror, "hint")).getSimpleName().toString());
                                if(hint==Hint.NONE)
                                    throw new AnnotationError("");
                                hintName = hint.key();
                            }
                            lhs = method.getEnclosingElement().getSimpleName()+"."+method.getSimpleName()+"."+hintName;
                        }
                        Element clash = entries.put(lhs, method);
                        if(clash!=null){
                            String signature;
                            if(clash instanceof ExecutableElement)
                                signature = ModelUtil.signature((ExecutableElement)clash, false);
                            else if(clash instanceof TypeElement)
                                throw new AnnotationError(method, "key '"+lhs+"' is already used by \""+((TypeElement)clash).getQualifiedName());
                            else
                                signature = clash.getSimpleName().toString();
                            throw new AnnotationError(method, "key '"+lhs+"' is already used by \""+signature+"\" in "+ clash.getEnclosingElement());
                        }

                        writeProperty(props, lhs, rhs);
                    }else{
                        String comment = ModelUtil.getAnnotationValue(method, entryMirror, "value");
                        if(comment.length()>0)
                            writeComments(props, comment);
                        else
                            props.newLine();
                    }
                }
            }
            props.newLine();
        }
    }
}