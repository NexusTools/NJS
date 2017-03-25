var Assert = importClass("org.junit.Assert");
var System = importClass("java.lang.System");

var NewClass = new JavaClass("net.nexustools.njs.Test", "java.lang.Runnable", {
    constructor: function(james) {
        this.james = james;
    },
    run: function() {
        System.out.println(this.james);
    }
});