## illustration of using cdt for parsing cpp

We relies on cdt to parse cpp header and source files.And the most important class of cdt is **GPPLanguage** that is concrete ILanguageimplementation for the DOM C++ parser.What's more,it extends org.eclipse.cdt.core.dom.parser.AbstractCLikeLanguage.
There is six static significant attributes and two methods in GPPLanguage.They will be described in detail  below.
      
1. **OPTION_SKIP_FUNCTION_BODIES**
Field will instruct the parse to skip function and method bodies.It will gain high performance,so we absolutely use it.
2. **OPTION_PARSE_INACTIVE_CODE**
Field will instruct the parser to create ast nodes for inactive code branches.And we also will use it
3. *OPTION_IS_SOURCE_UNIT*
4. *OPTION_NO_IMAGE_LOCATIONS*
5. *OPTION_ADD_COMMENTS*
6. *OPTION_SKIP_TRIVAL_EXPRESSIONS_IN_AGGREGATE_INITIALIZERS*


Although we only use first and second fields,others are important too.However,we do not need now, so we will not describe.
Equally important,we as well as need two methods

1. **getDefault()**
This method will construct a GPPLanguage object.
2. **getASTTranslationUnit()**
This method of the GPPLanguage's object will parse source and header files and return a IASTTranslationUnit object.Thus,we will gain our syntax tree.Then we are able to extract cpp symbols.


For now,we would extract macro,functions,global variables,enumeration,composite type including struct, union and class,using declaration,typedef declaration,namespace,template declaration.

* ![macro icon](../symbols/ui/icon/macro_obj.png)macro

 ```c
 #define PRINT(message) {
    printf(message);
 }
 ```
     
* ![function icon](../symbols/ui/icon/function_obj.png)function
 
 ```c	 
		  void (*signal5(int sig, void (*func)(int),char ch[][]))(int);
 ```

* ![variable icon](../symbols/ui/icon/variable_obj.png)variable

 ```c
 struct Person p;
 const int a;
 ```

* ![enum icon](../symbols/ui/icon/enum_obj.png)enum

 ```c
 enum strategy {RANDOM, IMMEDIATE, SEARCH};
 ```


* ![struct icon](../symbols/ui/icon/struct_obj.png)struct

 ```c
struct Person {
    static char* name;
    struct Address {
        const char *country,*province;
    } address;
} people;
```

* ![union icon](../symbols/ui/icon/union_obj.png)uion

 ```c
 union test {
   int a;
   void fun1();
   static int func3()const;
  };
```

* ![class icon](../symbols/ui/icon/class_obj.png)class

 ```c
 class Point{
     Point();
     Point operator++();
     Point operator+();
  };
```

* ![using icon](../symbols/ui/icon/namespace_obj.png)using

 ```c
  using namespace std;
 ```

* ![typedef icon](../symbols/ui/icon/typedef_obj.png)typedef

 ```c
  typedef struct {
      int age;
      char* name;
  } Person;
 ```

* ![namespace icon](../symbols/ui/icon/namespace_obj.png)namespace

 ```c
  namespace two {
    int a;
    void test(int a);
  }
 ```

* ![template icon](../symbols/ui/icon/function_obj.png)template

 ```c
  template<typename T>
  int main(int argc, char argv[][]){
       return 0;
  }
 ```

As for new features will be supplemented later.

In terms of Problem nodes,they will be appended to the Logger's object.	

Moreover, cpp14 features we handle some of them.For example,keywords auto and constexpr will be extracted.

 ```c
  auto find_id(const std::vector<record> &people) {
    auto match_name = [&name](const record& r) -> bool {
      return r.name == name;
    };
 }
 constexpr int a; 
 ```	 


And our icons copy from eclipse.
   	 
***    	 
*We hereby declare that most of the icons we use in cpp take from Visual Studio Library2013. No other than template fields of class is consist of template icon and fields icon.*
***
That is all the instructions.