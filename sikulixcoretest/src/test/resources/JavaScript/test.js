//Commands.popup("hello");
var reg = new Element(100, 200, 300, 400);
//use();
print("var reg = new Element(100, 200, 300, 400)");
print("reg.toJson():", reg.toJson())
//jreg = jsonToJava(reg.toJson());
//print("Java:", jreg);
print("toJson(fromJson(reg.toJson())):", toJson(fromJson(reg.toJson())));
print("click(reg): ", click(reg));
print("click(\"img.png\", reg): ", click("img.png", reg));
print("click(10, 10): ", click(10, 10));

