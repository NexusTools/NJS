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
public abstract class NumberObject implements BaseObject {

    Number Number;

    public NumberObject(Number Number) {
        this.Number = Number;
    }

    @Override
    public byte toByte() {
        return toNumber().toByte();
    }

    @Override
    public short toShort() {
        return toNumber().toShort();
    }

    @Override
    public int toInt() {
        return toNumber().toInt();
    }

    @Override
    public long toLong() {
        return toNumber().toLong();
    }

    @Override
    public Number.Instance toNumber() {
        try {
            try {
                return Number.wrap(Double.valueOf(toString()));
            } catch (NumberFormatException ex) {
                return Number.NaN;
            }
        } catch(NullPointerException ex) {
            System.out.println(getClass());
            throw ex;
        }
    }

    @Override
    public double toDouble() {
        return toNumber().toDouble();
    }

    @Override
    public float toFloat() {
        return toNumber().toFloat();
    }

}
