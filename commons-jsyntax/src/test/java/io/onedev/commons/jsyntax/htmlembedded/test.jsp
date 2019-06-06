<%@page import="lmt.sy4.factory.DAOFactory"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta charset="UTF-8">
		<title>帖子浏览</title>
		<link href="./CSS/global.css" type="text/css" rel="stylesheet" />
		<link href="./CSS/detail.css" type="text/css" rel="stylesheet" />
		<script type="text/javascript" src="./JS/jquery-1.8.3.min.js"></script>
	</head>
	
	<body>
		<div id="TopBar">
			<div>
				<div style="position:relative;float:right;margin-top:25px;margin-right:20px;">
					<iframe frameborder="0" width="180" height="36" scrolling="no" src="http://tianqi.2345.com/plugin/widget/index.htm?s=3&z=2&t=0&v=0&d=1&bd=0&k=&f=ffffff&q=1&e=0&a=0&c=58027&w=180&h=36&align=center"></iframe>
				</div>
				<%
					if (session.getAttribute("user") == null) {
						%>
				<div class="BarButton">
					<a href="./login.jsp" style="color:white;" >登录</a>
				</div>
				<%
					}
					else {
						%>
				<div class="BarButton">
					<a href="./logout.jsp" style="color:white;" >注销</a>
				</div>
				<%
					}
				%>
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
		
		<br /><br /><br /><br />
		
		<%@page import="lmt.sy4.vo.ReplyPost" %>
		<%@page import="lmt.sy4.vo.Post" %>
		<%@page import="java.util.List" %>
		<%
			int pid = Integer.parseInt(request.getParameter("id"));
			session.setAttribute("pid", pid);
			Post post = DAOFactory.getIPostDAOInstance().findById(pid);
			List<ReplyPost> lst = DAOFactory.getIReplyPostDAOInstance().findByPid(pid);
		%>
		<div class="titleBar"><%=post.getTitle() %></div>
		
		<div class="PostBar">
			<div class="BarImgLeft">
				<img src="./IMAGE/type1.png" width="135" height="110" />
			</div>
			<div class="PostContent">
				<svg class="triangle trileft">
					<polygon points="0,30 30,15 30,45" />
				</svg>
				<b><%=post.getUsername() %></b><br /><hr style="border:1px solid white" />
				<%=post.getContent() %>
				<br /><br />
				<div class="timediv" style="text-align:center"><%=post.getTime() %></div>
			</div>
		</div>
		
		<%
			int i = 0;
			for (ReplyPost rep : lst) {
				i++;
				%>
		<div class="PostBar">
			<%
				if (i % 2 == 0) {
					%>
			<div class="BarImgLeft">
				<img src="./IMAGE/type1.png" width="135" height="110" />
			</div>
			<div class="PostContent">
				<svg class="triangle trileft">
					<polygon points="0,30 30,15 30,45" />
				</svg>
				<b><%=rep.getUsername() %></b><br /><hr style="border:1px solid white" />
			<%
				} else {
					%>
			</div>
			<div class="BarImgRight">
				<img src="./IMAGE/type2.png" width="115" height="100" />
			</div>
			<div class="PostContent">
				<svg class="triangle triright">
					<polygon points="30,30 0,15 0,45" />
				</svg>
				<div style="float:right"><b><%=rep.getUsername() %></b></div><br /><hr style="border:1px solid white" />
			<%
				}
			%>
			<%=rep.getContent() %>
				<br /><br />
				<div class="timediv" style="text-align:center"><%=rep.getTime() %></div>
			</div>
		</div>
		<%
			}
		%>
		
		<br /><br />
		
		<form id="RePostForm" method="post" action="./reply">
			<textarea name="content" class="TextArea"></textarea>
			<div id="repostBtnPos">
				<input type="submit" class="Button" value="回帖" />
			</div>
		</form>
	</body>
</html>
