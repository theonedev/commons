<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta charset="UTF-8">
		<title></title>
		<link href="./CSS/global.css" type="text/css" rel="stylesheet" />
		<link href="./CSS/index.css" type="text/css" rel="stylesheet" />
		<link href="./PLUGIN/spig/css/spigPet.css" type="text/css" rel="stylesheet" />
		<script type="text/javascript" src="./PLUGIN/spig/js/jquery.min.js"></script>
    	<script type="text/javascript" src="./PLUGIN/spig/js/spig.js"></script>
		<script type="text/javascript" src="./JS/jquery-1.8.3.min.js"></script>
		<script type="text/javascript">
        	var visitor = 9;
        		if (session.getAttribute("user") == null) {
        			out.print("游客");
        		}
        		else {
        			out.print(((User)session.getAttribute("user")).getUsername());
        		}
        	var title = document.title;
			function down(id) {
				$("#content" + id).fadeIn(600);
			}
			function up(id) {
				$("#content" + id).fadeOut(600);
			}
		</script>
		<style type="text/css">
			.triangle
			{
				position: relative;
				width: 30px;
				height: 45px;
			}
			
			.trileft
			{
				float: left;
				margin-left: -41px;
			}
			
			.triright
			{
				float: right;
				margin-right: -41px;
			}
			
			.triangle polygon
			{
				fill: rgba(211, 246, 252, 0.5);
				stroke: rgb(35, 136, 240);
				stroke-width: 2;
			}
			
			#RePostForm
			{
				position: relative;
				margin-left: auto;
				margin-right: auto;
				width: 520px;
				height: 400px;
			}
			
			#repostBtnPos
			{
				position: relative;
				margin-top: 10px;
				margin-left: auto;
				margin-right: auto;
				text-align: center;
			}
		</style>
	</head>
	
	<body>
		<div id="TopBar">
			<div>
				<div style="position:relative;float:right;margin-top:25px;margin-right:20px;">
					<iframe frameborder="0" width="180" height="36" scrolling="no" src="http://tianqi.2345.com/plugin/widget/index.htm?s=3&z=2&t=0&v=0&d=1&bd=0&k=&f=ffffff&q=1&e=0&a=0&c=58027&w=180&h=36&align=center"></iframe>
				</div>
				<div class="BarButton">
					<a href="./login.jsp" style="color:white;" >登录</a>
				</div>
				<div class="BarButton">
					<a href="./logout.jsp" style="color:white;" >注销</a>
				</div>
				<div class="BarButton">
					<a href="./info.jsp" style="color:white;" >个人信息</a>
				</div>
				<div class="BarButton">
					<a href="./index.jsp" style="color:white;" >首页</a>
				</div>
				<div id="title">
				</div>
			</div>
		</div>
		<div id="spig" class="spig">
			<div id="message">正在加载中……</div>
			<div id="mumu" class="mumu"></div>
		</div>
		<br /><br /><br /><br />
		
		<div class="SectionBar">
			<div class="BarImgLeft">
				<img src="./IMAGE/type1.png" width="135" height="110" />
			</div>
			<div class="SectionBarContent">
				<div class="BarTitle">基础算法</div>
				<div class="LinkRight">
					<a href="javascript:down(1)">Down</a>
					<a href="javascript:up(1)">Up</a>
				</div>
			</div>
		</div>
		<div id="content1" class="SectionContent" style="display:block">
			<table class="tbl" align="center">
				<tr>
					<th>帖子</th><th>发帖人</th><th>时间</th>
				</tr>
				<tr>
					<td width="60%" align="center"><a href="./detail.jsp?id=<%=post.getId() %>"></a></td>
					<td align="center"></td>
					<td align="center"><div class="timediv"></div></td>
				</tr>
			</table>
		</div>
		<br /><br />
	</body>
</html>
/* C demo code */

#include <zmq.h>
#include <pthread.h>
#include <semaphore.h>
#include <time.h>
#include <stdio.h>
#include <fcntl.h>
#include <malloc.h>

typedef struct {
  void* arg_socket;
  zmq_msg_t* arg_msg;
  char* arg_string;
  unsigned long arg_len;
  int arg_int, arg_command;

  int signal_fd;
  int pad;
  void* context;
  sem_t sem;
} acl_zmq_context;

#define p(X) (context->arg_##X)

void* zmq_thread(void* context_pointer) {
  acl_zmq_context* context = (acl_zmq_context*)context_pointer;
  char ok = 'K', err = 'X';
  int res;

  while (1) {
    while ((res = sem_wait(&context->sem)) == EINTR);
    if (res) {write(context->signal_fd, &err, 1); goto cleanup;}
    switch(p(command)) {
    case 0: goto cleanup;
    case 1: p(socket) = zmq_socket(context->context, p(int)); break;
    case 2: p(int) = zmq_close(p(socket)); break;
    case 3: p(int) = zmq_bind(p(socket), p(string)); break;
    case 4: p(int) = zmq_connect(p(socket), p(string)); break;
    case 5: p(int) = zmq_getsockopt(p(socket), p(int), (void*)p(string), &p(len)); break;
    case 6: p(int) = zmq_setsockopt(p(socket), p(int), (void*)p(string), p(len)); break;
    case 7: p(int) = zmq_send(p(socket), p(msg), p(int)); break;
    case 8: p(int) = zmq_recv(p(socket), p(msg), p(int)); break;
    case 9: p(int) = zmq_poll(p(socket), p(int), p(len)); break;
    }
    p(command) = errno;
    write(context->signal_fd, &ok, 1);
  }
 cleanup:
  close(context->signal_fd);
  free(context_pointer);
  return 0;
}

void* zmq_thread_init(void* zmq_context, int signal_fd) {
  acl_zmq_context* context = malloc(sizeof(acl_zmq_context));
  pthread_t thread;

  context->context = zmq_context;
  context->signal_fd = signal_fd;
  sem_init(&context->sem, 1, 0);
  pthread_create(&thread, 0, &zmq_thread, context);
  pthread_detach(thread);
  return context;
}
[ This program prints "Hello World!" and a newline to the screen, its
  length is 106 active command characters [it is not the shortest.]

  This loop is a "comment loop", it's a simple way of adding a comment
  to a BF program such that you don't have to worry about any command
  characters. Any ".", ",", "+", "-", "<" and ">" characters are simply
  ignored, the "[" and "]" characters just have to be balanced.
]
+++++ +++               Set Cell #0 to 8
[
    >++++               Add 4 to Cell #1; this will always set Cell #1 to 4
    [                   as the cell will be cleared by the loop
        >++             Add 2 to Cell #2
        >+++            Add 3 to Cell #3
        >+++            Add 3 to Cell #4
        >+              Add 1 to Cell #5
        <<<<-           Decrement the loop counter in Cell #1
    ]                   Loop till Cell #1 is zero; number of iterations is 4
    >+                  Add 1 to Cell #2
    >+                  Add 1 to Cell #3
    >-                  Subtract 1 from Cell #4
    >>+                 Add 1 to Cell #6
    [<]                 Move back to the first zero cell you find; this will
                        be Cell #1 which was cleared by the previous loop
    <-                  Decrement the loop Counter in Cell #0
]                       Loop till Cell #0 is zero; number of iterations is 8

The result of this is:
Cell No :   0   1   2   3   4   5   6
Contents:   0   0  72 104  88  32   8
Pointer :   ^

>>.                     Cell #2 has value 72 which is 'H'
>---.                   Subtract 3 from Cell #3 to get 101 which is 'e'
+++++++..+++.           Likewise for 'llo' from Cell #3
>>.                     Cell #5 is 32 for the space
<-.                     Subtract 1 from Cell #4 for 87 to give a 'W'
<.                      Cell #3 was set to 'o' from the end of 'Hello'
+++.------.--------.    Cell #3 for 'rl' and 'd'
>>+.                    Add 1 to Cell #5 gives us an exclamation point
>++.                    And finally a newline from Cell #6
