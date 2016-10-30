var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

var fruit = ['cherries', 'apples', 'bananas'];
Assert.assertTrue(fruit.sort().toString() === "apples,bananas,cherries")

var scores = [1, 10, 21, 2];
Assert.assertTrue(scores.sort().toString() === "1,10,2,21");

var things = ['word', 'Word', '1 Word', '2 Words'];
Assert.assertTrue(things.sort().toString() === "1 Word,2 Words,Word,word");

function bob() {
	return arguments;
}

Assert.assertTrue(Array.prototype.sort.call(bob('z', 'c', 'e', 'a', 'b'))[1] === "b");

var numbers = [4, 2, 5, 1, 3];
numbers.sort(function(a, b) {
	return a - b;
});
Assert.assertTrue(numbers.toString() === "1,2,3,4,5");

function Entry(name, value) {
	Object.defineProperty(this, "name", {
		value: name
	});
	Object.defineProperty(this, "value", {
		value: value
	});
}
Entry.prototype.toString = function() {
	if(this.value)
		return this.name + ":" + this.value;
	return this.name;
};

var items = [
	new Entry('Edward', 21),
	new Entry('Sharpe', 37),
	new Entry('And', 45),
	new Entry('The', -12),
	new Entry('Magnetic'), // Sorting will stop here due to a NaN
	new Entry('Zeros', 37)
];
// sort by value
items.sort(function (a, b) {
  return a.value - b.value;
});
Assert.assertTrue(items.toString() === "The:-12,Edward:21,Sharpe:37,And:45,Magnetic,Zeros:37");

items.sort(function(a, b) {
  var nameA = a.name.toUpperCase(); // ignore upper and lowercase
  var nameB = b.name.toUpperCase(); // ignore upper and lowercase
  
  if (nameA < nameB)
    return -1;
  if (nameA > nameB)
    return 1;

  // names must be equal
  return 0;
});
Assert.assertTrue(items.toString() === "And:45,Edward:21,Magnetic,Sharpe:37,The:-12,Zeros:37");