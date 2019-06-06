## illustration of using sonarsource javascript-frontend for parsing flow type annotation
We relies on sornarsource javascript-frontend to parse js source code.In version 4.1,we can parse flow type annotation in js script file.And the most important argument is **EcmaScriptLexer.SCRIPT** when we want to extract flow type annotation.In version 4.1,it offers one interface called JavaScriptParserBuilder.Moreover, there are only three construct methods.And their only difference is parameter.They will be discribed in detail below.

1. **NULL**
Null represents we only parse general js source code.And we drop 
2. **EcmaScriptLexer.SCRIPT**.And we will use this construct method to parse js source code with flow type annotation.

3. **EcmaScriptLexer.VUE_SCRIPT**.This parameter represents it will parse JS Vue source code.However, we may not use it.

For now,we would extract general js code,flow type annotation and so on.

* ![class icon](../symbols/ui/icon/class.png)general class

```js
class Polygon {
  constructor(height, width) {
    this.name = 'Polygon';
    this.height = height;
    this.width = width;
  }
  test() {
    class Inner1 {
      method1() {
      }
    }
  }
}
```

* ![icon](../symbols/ui/icon/object.png)common js

```js
var foo = require('./foo');
exports.app = function(){
    console.log('Im an application!');
}
```

* ![icon](../symbols/ui/icon/function.png)jquery

```js
(function ( $ ) {
 
    $.fn.greenify = function() {
        this.css( "color", "green" );
        return this;
    };
 
}( jQuery ));
```

* ![icon](../symbols/ui/icon/exported_function.png)module

```js
import person from 'person';
export function test() {
	this.name = 'robin';
};
```

* ![icon](../symbols/ui/icon/object.png)object-literal

```js
var person = {
  name: 'a person',
  get grade() {
    return 12;
  },
  set grade(grade) {
  },
};
```

* ![icon](../symbols/ui/icon/function.png)typeAnnotation example1

```js
function test(value:test, value1 : bool){
}
function accessTwo(value:2){
}
function accessMixed(value:mixed){	
}
function accessAny(name:any){
}
function acceptsMaybeString(value:?string){
}
function concat(a:string,b:string):string{
	return a + b;
}
```

* ![icon](../symbols/ui/icon/object.png)typeAnnotation example2 

```js
var obj1:{foo:boolean} = {foo:true};
let arr:Array<number> = [1,2,3];
let tuple:[number] = [1];
```

* ![icon](../symbols/ui/icon/class.png)typeAnnotation example3

```js
class MyClass{
	prop:number;
    method(value:string):number{	
    }
}
```

* ![icon](../symbols/ui/icon/object.png)typeAnnotation example4

```js
type MyObject<A, B> = {
		foo:A,
		bar:B,
};
```

* ![icon](../symbols/ui/icon/function.png)typeAnnotation example5

```js
opaque type ID = string;
function identify(x:ID):ID{
	return x;
}
```

* ![icon](../symbols/ui/icon/object.png)typeAnnotation example6

```js
interface Serializable{
    serializable(value:test,value1:test):string	
}
```

* ![icon](../symbols/ui/icon/function.png)typeAnnotation example7

```js
function identify<T>(value:T):T{
	return value;
}
function toStringPrimitives(value:number|boolean){
	return String(value);
}
function method(value:A&B&C){
}
```

* ![icon](../symbols/ui/icon/function.png)typeAnnotation example8

```js
function identify<T>(value:T):T{
	return value;
}
function toStringPrimitives(value:number|boolean){
	return String(value);
}
function method(value:A&B&C){
}
```

* ![icon](../symbols/ui/icon/object.png)variable

```js
  var person = {
    name: "robin",
  }
  person.age = 100;
  module.exports = person;
  exports = hello.world;
```

* ![icon](../symbols/ui/icon/object.png)vue

```js
Vue.component('child', {
  props: ['message'],
  template: '<span>{{ message }}</span>'
});

new Vue({
  components: {
    'my-component': Child
  }
});
```

* ![icon](../symbols/ui/icon/function.png)vuejs

```js
export const createEmptyVNode = (text: string = '') => {
  const node = new VNode()
  node.text = text
  node.isComment = true
  return node
}
export function createTextVNode (val: string | number) {
  return new VNode(undefined, undefined, undefined, String(val))
}
```

That is all the instructions.

