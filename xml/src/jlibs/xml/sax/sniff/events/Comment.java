/**
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

package jlibs.xml.sax.sniff.events;

import jlibs.xml.sax.sniff.engine.data.LocationStack;

/**
 * @author Santhosh Kumar T
 */
public class Comment extends Event{
    public Comment(DocumentOrder documentOrder, LocationStack locationStack){
        super(documentOrder, locationStack);
    }

    @Override
    public int type(){
        return COMMENT;
    }

    @Override
    public String location(){
        return locationStack.comment();
    }

    @Override
    protected String value(){
        return new String(ch, start, length);
    }

    @Override
    public String localName(){
        return null;
    }

    @Override
    public String namespaceURI(){
        return null;
    }

    @Override
    public String qualifiedName(){
        return null;
    }

    public char[] ch;
    public int start;
    public int length;

    public void setData(char[] ch, int start, int length){
        this.ch = ch;
        this.start = start;
        this.length = length;
        hit();
    }

    @Override
    public String toString(){
        return new String(ch, start, length);
    }
}
