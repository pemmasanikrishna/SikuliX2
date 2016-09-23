//Commands.popup("hello");
var reg = new Region(100, 200, 300, 400);
use();
print("Java:", reg.toJson())
jreg = jsonToJava(reg.toJson());
print("Java:", jreg);
print("JavaScript:", toJson(fromJson(reg.toJson())));
click(jreg);
print("clicked: ", click("img.png", reg));
print("clicked: ", click(10, 10));

