/* 
 * Copyright (c) 2016 NexusTools.
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU Lesser General Public License as   
 * published by the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * Lesser General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nexustools.njs.compiler;

/**
 *
 * @author kate
 */
public class SourceBuilder {

        java.lang.String indent = "";
        StringBuilder builder = new StringBuilder();

        public void append(java.lang.String source) {
                assert (source.indexOf('\n') == -1);
                builder.append(source);
        }

        public void appendln(java.lang.String source) {
                append(source);
                builder.append('\n');
                builder.append(indent);
        }

        public void appendln() {
                builder.append('\n');
                builder.append(indent);
        }

        public void indent() {
                indent += '\t';
                if(Character.isWhitespace(builder.charAt(builder.length() - 1)))
                        builder.append('\t');
        }

        public void unindent() {
                int pos = builder.length() - 1;
                indent = indent.substring(0, indent.length() - 1);
                if(Character.isWhitespace(builder.charAt(pos)))
                        builder.deleteCharAt(pos);
        }

        @Override
        public java.lang.String toString() {
                return builder.toString(); //To change body of generated methods, choose Tools | Templates.
        }
}
