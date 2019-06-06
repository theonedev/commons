## What is JSyntax
JSyntax is an open source Java library doing syntax highlighting for commonly used programming languages. We implement it by translating various [CodeMirror](http://codemirror.net) language modules from JavaScript to Java, and use it in [GitPlex](https://www.pmease.com/gitplex) to do server side syntax highlighting when calculate diffs between two revisions. 

## How to use
[This unit test](https://www.gitplex.com/gitplex/jsyntax/blob/master/src/test/java/com/gitplex/jsyntax/ToHtmlTest.java) demonstrates how to tokenize code as syntax tokens, and turn tokens into html. The result will be the same as produced by CodeMirror and can be themed with various CodeMirror themes. 

## How to contribute
1. Sign up an account at https://www.gitplex.com, and fork this repository (fork link can be found by clicking the ellipsis icon besides the repository name)
1. Clone the forked repository to your machine
1. Open Eclipse and make sure JRE/JDK 1.8.0_91 or higher is used as default JRE (setting resides in _Preferences/Java/Installed JRE_)
1. Import the contained Maven project into Eclipse
1. Make sure everything works fine by running contained unit tests
1. Pick a language not assigned from [language list below](https://www.gitplex.com/gitplex/jsyntax/blob#list-of-language-modes), and translate it into corresponding Tokenizer class following [this guide](https://www.gitplex.com/gitplex/jsyntax/blob#language-translation-guide). The translated Tokenizer class should satisfy below requirements:
    * It should be translated against CodeMirror version [5.21.0](https://github.com/codemirror/CodeMirror/tree/5.21.0/mode)
    * It should extend from [AbstractTokenizer](https://www.gitplex.com/gitplex/jsyntax/blob/master/src/main/java/com/gitplex/jsyntax/AbstractTokenizer.java)
    * It should reside in a package with the same name as the corresponding CodeMirror mode directory
	* It should have a default constructor so that [TokenizerRegistry](https://www.gitplex.com/gitplex/jsyntax/blob/master/src/main/java/com/gitplex/jsyntax/TokenizerRegistry.java) can instantiate it
	* Its instance should be thread safe
	* It should product the same result as its CodeMirror counterpart when test against any snippet of targeting language. To do this, create an unit test extending from [AbstractTokenizerTest](https://www.gitplex.com/gitplex/jsyntax/blob/master/src/test/java/com/gitplex/jsyntax/AbstractTokenizerTest.java) to call the verify method using typical language sources
1. After finishing the work, push the changes and send a pull request to original repository [gitplex/jsyntax](https://www.gitplex.com/gitplex/jsyntax)

## List of language modes
Below table corresponds to [CodeMirror 5.21.0 mode list](https://github.com/codemirror/CodeMirror/tree/5.21.0/mode). Checked items are already translated, and those with developer name are already assigned. 

- [ ] apl
- [x] asciiarmor
- [ ] asn.1
- [ ] asterisk
- [x] brainfuck
- [x] clike
- [ ] clojure
- [x] cmake
- [x] cobol
- [ ] coffeescript
- [x] commonlisp
- [ ] crystal
- [x] css
- [ ] cypher
- [x] d
- [x] dart
- [x] diff
- [ ] django
- [x] dockerfile
- [ ] dtd
- [ ] dylan
- [ ] ebnf
- [x] ecl
- [ ] eiffel
- [ ] elm
- [x] erlang
- [x] factor
- [ ] fcl
- [ ] forth
- [x] fortran
- [ ] gas
- [ ] gfm
- [ ] gherkin
- [x] go
- [x] groovy
- [ ] haml
- [ ] handlebars
- [ ] haskell
- [ ] haskell-literate
- [x] haxe
- [ ] htmlembedded
- [x] htmlmixed
- [x] http
- [ ] idl
- [x] javascript
- [ ] jinja2
- [ ] jsx
- [ ] julia
- [ ] livescript
- [x] lua
- [ ] markdown
- [x] mathematica
- [ ] mbox
- [x] mirc
- [ ] mllike
- [x] modelica
- [ ] mscgen
- [ ] mumps
- [ ] nginx
- [ ] nsis
- [ ] ntriples
- [ ] octave
- [ ] oz
- [x] pascal
- [x] pegjs
- [x] perl
- [x] php
- [x] pig
- [ ] powershell
- [x] properties
- [x] protobuf
- [x] pug
- [ ] puppet
- [x] python
- [x] q
- [x] r
- [ ] rpm
- [ ] rst
- [x] ruby
- [x] rust
- [ ] sas
- [ ] sass
- [x] scheme
- [x] shell
- [x] sieve
- [x] slim
- [x] smalltalk
- [ ] smarty
- [x] solr
- [x] soy
- [ ] sparql
- [ ] spreadsheet
- [ ] sql
- [ ] stex
- [x] stylus
- [x] swift
- [x] tcl
- [ ] textile
- [ ] tiddlywiki
- [ ] tiki
- [x] toml
- [x] tornado
- [ ] troff
- [ ] ttcn
- [ ] ttcn-cfg
- [x] turtle
- [ ] twig
- [x] vb
- [x] vbscript
- [ ] velocity
- [x] verilog
- [ ] vhdl
- [ ] vue
- [ ] webidl
- [x] xml
- [ ] xquery
- [ ] yacas
- [ ] yaml
- [ ] yaml-frontmatter
- [x] z80

## Language translation guide
Translating a language is not a tough task if you understand typical JavaScript constructs and knows how to translate it into Java counterparts. You do not need to understand syntax of the target language. We've translated some language modes with unit tests, and here we explain some typical CodeMirror constructs as well as translation guidelines using the clike mode as an example:

1. When CodeMirror highlights syntax for a particular language mode, only two files are involved, the entry file [runmode-standalone.js](https://github.com/codemirror/CodeMirror/blob/5.21.0/addon/runmode/runmode-standalone.js) and the corresponding mode file. The equivalent Java class for _runmode_standalone.js_ in JSyntax is [AbstractTokenizer](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/AbstractTokenizer.java). For c-like languages (C, C++, CSharp, etc.), the mode file in CodeMirror is [clike.js](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js). 
1. CodeMirror often processes similiar languages (C, C++, etc) in a single mode file, and uses the _def_ construct to provide language specific properties. For instance, c++ language is supported in clike.js via [c++ def block](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L380-L420). It overrides some common properties to provide c++ specific keywords, types, or hooks. So for clike languages, we create an abstract class [ClikeTokenizer](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java) to hold common logics, and for each language specific definition block, we create a concrete class ([CppTokenizer](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/clike/CppTokenizer.java) for example) to provide language specific properties and behaviors. 
1. The [defineMode construct](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L50-L67) defines properties and behaviors overridable by different languages via parseConfig object. For each of these, we define a method in ClikeTokenizer and provide sensible default value. For instance, the [indentStatements](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L61) definition indicates that the value is _true_ unless it is explicitly specified as _false_ in specific language. Hence we have [this method](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=142.0-144.2) defined in JSyntax to match property _indentStatements_. 
1. The [StartState](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L156-L164) function discloses what information needs to be put into tokenizer state class which is expected by [parameter S](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/AbstractTokenizer.java&mark=13.39-13.42) of our AbstractTokenizer. We carefully deduced type of each property and also put some global variables (such as [curPunc and isDefKeyword](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L69)) into this class to make the tokenizer instance thread safe. 
1. The [Context function](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L14-L21) looks like a simple data stucture, so we define a corresponding class [Context](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=49.0-66.2) in JSyntax. Type of each properties of _Context_ class is carefully determined by looking at property usages in _clike.js_. For instance, type of property _align_ is _Boolean_ instead of _boolean_ as we find that its initial value is _null_ instead of _false_ according to [this line](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L169)
1. The [stream object](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L166) in clike.js corresponds to class [StringStream](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/StringStream.java) in JSyntax, with method explanations below:
    * Call _!stream.match(pattern).isEmpty()_ or _!stream.match(pattern, consume).isEmpty()_ to check if the stream matches a pattern. For instance [this line](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L179) is translated as [this line](https://www.gitplex.com/gitplex/jsyntax/blob/5d21d81e97b6230009b6bfdd88fb94acdcac2831/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=333.0-334.0)
	* Methods [string()](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/StringStream.java&mark=50.0-53.0), [peek()](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/StringStream.java&mark=66.0-72.0), [next()](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/StringStream.java&mark=73.0-78.2), [eat(match)](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/StringStream.java&mark=80.0-88.2), [eat(pattern)](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/StringStream.java&mark=90.0-98.2), and [current()](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/StringStream.java&mark=186.0-189.0) will never return value _null_. An empty string will be returned to indicate nothing. So [this line](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L91) should be translated [this way](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=205.0-206.0). 
1. Implementation of Method [AbstractTokenizer.token(stream, state)](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/AbstractTokenizer.java&mark=39.0-40.0) should return an emtpy string 	to indicate nothing. So the _null_ returning logic [here](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L173) should be translated [this way](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=315.0-315.13). For any other methods returning string, we also suggest to follow this convention to avoid _null_ polution. 
1. In clike.js, various hook functions can be assigned and called later. So we create an interface [Processor](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=68.0-71.0) and translate various hooks as implementations of the hook. For instance, the function [cppHook](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L267-L280) is translated to class [CppHook](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=384.0-409.2)
1. For performance reasons, all JavaScript regular expressions should be translated into compiled Pattern in Java. For instance, regular expression in [this line](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L66) is translated to [this form](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/clike/ClikeTokenizer.java&mark=24.0-25.0). Also note that regular expression of Java and JavaScript is almost the same. Normally you only need to change back slash in JavaScript as double slashes in Java string.
1. The [TokenizerUtils](https://www.gitplex.com/gitplex/jsyntax/blob/3229770cba3ee6fb17187db140c22f530a3ac93f/src/main/java/com/gitplex/jsyntax/TokenizerUtils.java) provides some utility methods to mimic some JavaScript string operations, such as substr, slice, etc. 
1. Method [wordsOf()](https://www.gitplex.com/gitplex/jsyntax/blob/8f9dabbe188b1009d608b0e63cec0de2b4d58738/src/main/java/com/gitplex/jsyntax/AbstractTokenizer.java&mark=62.0-75.0) in AbstractTokenizer can be used to construct a Set from space delimitted string quickly, which will be useful when translate keyword sets in many language modes.
1. Some logic is for other purposes, and we simply discard them. For instance, [this](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L4-L12) and [this](https://github.com/codemirror/CodeMirror/blob/5.21.0/mode/clike/clike.js#L215-L249)
1. Unit tests verifies syntax highlighting result of translated tokenizer against the original CodeMirror language mode, and will fail if they do not match. CodeMirror will run in Nashorn to do syntax highlighting. 

## How to debug
Since we only perform language level construct translations without understanding the real syntax highlighting logics, it is difficult to locate the problem when unit test fails. In this case, you may reduce the source file failing the unit test to the minimum (CodeMirror works even for partial source, so don't worry if source is reduced to a few words), and use this minimized source to debug CodeMirror highlighting logic and your translated logic to find out where the problem is. You know how to debug your Java program, and here is how you debug CodeMirror highlighting logic:

1. Find class WebServer in the project and run it as a Java program
1. Point your browser to http://localhost:8080 and copy the minimized source as _File Content_
1. Select appropriate _File Type_ and hit the highlight button to highlight the source
1. Now you have a CodeMirror instance running in your browser doing syntax highlighting, and you may debug the entry file ([runmode-standalone.js](https://github.com/codemirror/CodeMirror/blob/5.21.0/addon/runmode/runmode-standalone.js)) or corresponding language mode file using JavaScript debugging facilities provided by browser

## FAQ
#### How JSyntax is licensed
JSyntax is open source and licensed under [Apache license](https://www.gitplex.com/gitplex/jsyntax/blob/master/license.txt)
#### Why don't you host JSyntax at GitHub
We are creating [GitPlex](https://www.pmease.com/gitplex) as a Git repository management server with unique features, and would like to eat our own dog food here
#### Why don't you run CodeMirror in Nashorn to do the job
Because that is too slow. However in our unit tests, we run CodeMirror in Nashorn to make sure that translated Java code produces the same result as CodeMirror