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

package jlibs.xml.sax.dog.path.tests;

import jlibs.xml.sax.dog.NodeType;
import jlibs.xml.sax.dog.path.Constraint;
import jlibs.xml.sax.dog.sniff.Event;

/**
 * @author Santhosh Kumar T
 */
public final class Star extends Constraint{
    public static final Star INSTANCE = new Star();

    private Star(){
        super(ID_STAR);
    }

    @Override
    public boolean matches(Event event){
        int type = event.type();
        return type==NodeType.ELEMENT || type==NodeType.ATTRIBUTE || type==NodeType.NAMESPACE;
    }

    @Override
    public String toString(){
        return "*";
    }
}
