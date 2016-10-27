function CustomError(message) {
     this.message = message;
	 var cstack = new Error().stack;
	 var pos = cstack.indexOf("\n");
	 if(pos > -1)
		this.stack = this.name + ": " + message + cstack.substring(pos);
	else
		this.stack = this.name + ": " + message;
 }
 CustomError.prototype = Object.create(Error.prototype);
 CustomError.prototype.name = "CustomError";
 CustomError.prototype.message = "";
 CustomError.prototype.constructor = CustomError;