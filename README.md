NexusTools JavaScript
=====================

This is a Java library which implements -almost all- ES5 functionality and some ES6.

It contains 2 compilers:
 - The default one generates Java Source files out of the JavaScript and compiles it using an available compiler if any.
 - The fallback one generates Java objects and runs the code like a basic interpretter, its very slow.
 
It is also possible to use this library to generate Java sources out of your JavaScript and include only the bare runtime without any compiler functionality.
This would hopefully allow you to include your generated sources in projects where runtime compilation is not allowed but you can still use Java.

License
-------
This project is licensed under the LGPLv3.
