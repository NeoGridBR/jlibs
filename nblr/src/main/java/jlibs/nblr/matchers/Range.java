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

package jlibs.nblr.matchers;

import jlibs.core.lang.StringUtil;

import java.util.Collections;
import java.util.List;

import static java.lang.Character.MAX_CODE_POINT;
import static java.lang.Character.MIN_SUPPLEMENTARY_CODE_POINT;

/**
 * @author Santhosh Kumar T
 */
public final class Range extends Matcher{
    public static final Range SUPPLIMENTAL = new Range(MIN_SUPPLEMENTARY_CODE_POINT, MAX_CODE_POINT);
    public static final Range NON_SUPPLIMENTAL = new Range(0, MIN_SUPPLEMENTARY_CODE_POINT-1);

    public final int from, to;

    public Range(String chars){
        int codePoints[] = StringUtil.toCodePoints(chars);
        from = codePoints[0];
        to = codePoints[2];
    }

    public Range(int from, int to){
        this.from = from;
        this.to = to;
        if(from>to)
            throw new IllegalArgumentException("invalid range: "+this);
    }

    @Override
    protected String __javaCode(String variable){
        return String.format("%s>=%s && %s<=%s", variable, toJava(from), variable, toJava(to));
    }

    public List<jlibs.core.util.Range> ranges(){
        return Collections.singletonList(new jlibs.core.util.Range(from, to)); 
    }

    @Override
    public String toString(){
        return '['+encode(from)+'-'+encode(to)+']';
    }
}
