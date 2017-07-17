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

var Assert = importClass("org.junit.Assert");

function* anotherGenerator(i) {
  yield i + 1;
  yield i + 2;
  yield i + 3;
}

function* generator(i) {
  yield i;
  yield* anotherGenerator(i);
  yield i + 10;
}

var gen = generator(10);

Assert.assertTrue(gen.next().value == 10);
Assert.assertTrue(gen.next().value == 11);
Assert.assertTrue(gen.next().value == 12);
Assert.assertTrue(gen.next().value == 13);
Assert.assertTrue(gen.next().value == 20);

function* logGenerator() {
  var a = yield;
  var b = yield;
  var c = yield;
  yield a+b+c;
}

var gen = logGenerator();

gen.next();
gen.next('pretzel');
gen.next('california');
gen.next('mayonnaise');
Assert.assertTrue(gen.next().value === "pretzelcaliforniamayonnaise");