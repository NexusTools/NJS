/*
 * Copyright (C) 2017 NexusTools.
 *
 * This library is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.0.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nexustools.njs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Katelyn Slater
 */
public class RegEx extends ConstructableFunction {

    public final Symbol.Instance pattern;
    public RegEx(final Global global) {
        super(global);
        
        pattern = global.Symbol.create("pattern");
        ((GenericObject)prototype).setHidden("test", new AbstractFunction(global) {
            @Override
            public BaseObject call(BaseObject _this, BaseObject... params) {
                Pattern pattern = (Pattern)((JavaObjectHolder)_this.get(RegEx.this.pattern)).javaObject;
                Matcher matcher = pattern.matcher(params[0].toString());
                return matcher.matches() ? global.Boolean.TRUE : global.Boolean.FALSE;
            }
        });
    }

    @Override
    public BaseObject call(BaseObject _this, BaseObject... params) {
        // TODO: Implement flags
        _this.set(pattern, new JavaObjectHolder(Pattern.compile(params[0].toString())));
        return _this;
    }
    
    public BaseObject create(java.lang.String pattern, java.lang.String flags) {
        BaseObject _this = _new();
        // TODO: Implement flags
        _this.set(this.pattern, new JavaObjectHolder(Pattern.compile(pattern)));
        return _this;
    }
    
}
