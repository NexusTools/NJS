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

/**
 *
 * @author Katelyn Slater <kate@nexustools.com>
 */
public interface BaseFunction extends BaseObject {

    public java.lang.String name();

    public java.lang.String source();

    public java.lang.String arguments();

    public BaseObject call(BaseObject _this, BaseObject... params);

    public BaseObject construct(BaseObject... params);

    public void setPrototype(BaseObject prototype);

    public BaseObject prototype();

    public BaseObject _new();
}
