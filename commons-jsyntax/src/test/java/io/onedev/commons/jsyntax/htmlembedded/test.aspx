<%@ Page Language="C#" AutoEventWireup="true" CodeFile="interaction.aspx.cs" Inherits="ykfw_interaction" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>游客互动</title>

    <link rel="stylesheet" href="../css/nzg.css" />
    <link rel="stylesheet" href="../css/jqjs1.css" />
    <link href="../css/whole.css" rel="stylesheet"/>
    <link media="screen" rel="stylesheet" href="../css/demo.css"/>
    <link rel="stylesheet" type="text/css" href="../uploadify/uploadify.css"/>
    <link rel="stylesheet" href="../FlexSlider/flexslider.css" />
    <style type="text/css">
        .Window
        {
            position: relative;
            margin-top: 200px;
            margin-left: auto;
            margin-right: auto;
            width: 500px;
            height: 400px;
            background: url(../img/caidan3.jpg) repeat-x ;
            background-color: white;
            font-family: 'Microsoft YaHei';
            font-size: medium;
        }
        .close
        {
            position: relative;
            width: 30px;
            height: 30px;
            top: 10px;
            left: 460px;
            background-image:url(../img/close.png);
            cursor: pointer;
        }
        .Input
        {
            position: absolute;
            width: 200px;
            height: 30px;
            font-family: 'Microsoft YaHei';
            font-size: medium;
        }
        .Button
        {
            position: absolute;
            background-color:rgb(205,92,92);
            border:none;
            width: 100px;
            height: 35px;
            cursor: pointer;
            font-family:'Microsoft Yahei';
            font-size: 19px;
            color:white;
        }
         .ButtonGo
        {
            position: absolute;
            background-color:white;
            border:none;
            width: 100px;
            height: 35px;
            cursor: pointer;
            font-family: 'Microsoft YaHei';
            font-size: 19px;
            color:rgb(205,92,92);
        }
        .tbl
        {
            width: 1000px;
        }
        .tbl td
        {
            width: 243px;
            height: 200px;
        }
        .photo
        {
            width: 243px;
            height: 200px;
            cursor: pointer;
        }
        .photo span
        {
            font-family: 'Microsoft YaHei';
            font-size: 14px;
            color: saddlebrown;
        }
    </style>

    <script type="text/javascript" src="../js/jquery-1.11.3.min.js"></script>
    <script type="text/javascript" src="../FlexSlider/jquery.flexslider-min.js"></script>
    <script type="text/javascript">
    jQuery(document).ready(function(){
	    var qcloud={};
	    $('[_t_nav]').hover(function(){
		    var _nav = $(this).attr('_t_nav');
		    clearTimeout( qcloud[ _nav + '_timer' ] );
		    qcloud[ _nav + '_timer' ] = setTimeout(function(){
		    $('[_t_nav]').each(function(){
		    $(this)[ _nav == $(this).attr('_t_nav') ? 'addClass':'removeClass' ]('nav-up-selected');
		    });
		    $('#'+_nav).stop(true,true).slideDown(200);
		    }, 150);
	    },function(){
		    var _nav = $(this).attr('_t_nav');
		    clearTimeout( qcloud[ _nav + '_timer' ] );
		    qcloud[ _nav + '_timer' ] = setTimeout(function(){
		    $('[_t_nav]').removeClass('nav-up-selected');
		    $('#'+_nav).stop(true,true).slideUp(200);
		    }, 150);
	    });
    });
    </script>
</head>

<body style="background-color: rgb(230,230,230)">

        <form id="form1" runat="server">
            <img src="../img/news.png" width="100%" height="100%" style="margin-top:100px;"/>
    <div id="allDiv">
        <div id="head" >
    <div  id="head-0">
        <div class="logo">         
        <img src="../img/logo.png" height="55px" width="176px" style="margin-top:10px">

        <iframe allowtransparency="true" frameborder="0" width="180" height="36" scrolling="no" src="http://tianqi.2345.com/plugin/widget/index.htm?s=3&z=2&t=0&v=0&d=1&bd=0&k=&f=ffffff&q=1&e=0&a=0&c=58027&w=180&h=36&align=center"></iframe>
            </div>
        </div>
<div id="head1">
<div class="head-v3">

	<div class="navigation-up">
	
		<div class="navigation-inner">
     
			<div class="navigation-v3">
				<ul>
					  <li class="nav-up-selected-inpage1" _t_nav="home">
						<h2>
							<a>&nbsp;</a>
						</h2>
					</li>
                   
                    <li class="nav-up-selected-inpage1" _t_nav="home">
						<h2>
							<a>&nbsp;</a>
						</h2>
					</li>
                     <li class="nav-up-selected-inpage1" _t_nav="home">
						<h2>
							<a></a>
						</h2>
					</li>

                    <li class="nav-up-selected-inpage1" _t_nav="home">
						<h2>
							<a href="../index.aspx">网站首页&nbsp;</a>
						</h2>
					</li>
					<li class="nav-up-selected-inpage1" _t_nav="product">
						<h2>
							<a href="../news/NewsDynamic.aspx">景区动态&nbsp;</a>
						</h2>
					</li>
					<li class="nav-up-selected-inpage1" _t_nav="wechat">
						<h2>
							<a href="../jqjs/jqjs_jqgk.aspx">景点博览&nbsp;</a>
						</h2>
					</li>
					<li class="nav-up-selected-inpage1" _t_nav="solution">
						<h2>
							<a href="#">智慧旅游&nbsp;</a>
						</h2>
					</li>
					<li class="nav-up-selected-inpage1" _t_nav="cooperate">
						<h2>
							<a href="#">游客服务&nbsp;</a>
						</h2>
					</li>
					<li class="nav-up-selected-inpage1" _t_nav="support">
						<h2>
							<a href="#">龟山文萃&nbsp;</a>
						</h2>
					</li>
                    <li class="nav-up-selected-inpage1" _t_nav="home">
						<h2>
							<a >&nbsp;</a>
						</h2>
					</li>
				     <li class="nav-up-selected-inpage1" _t_nav="home">
						 <h2>
							<a>&nbsp;</a>
						 </h2>
					 </li>                                        
                </ul>

			</div>
		</div>
	</div>
</div>
</div>
<div id="head2">
<div class="navigation-down" style="z-index:999;">
		<div id="product" class="nav-down-menu menu-3 menu-1" style="display: none;" _t_nav="product">
			<div class="navigation-down-inner">
			<div class="inner-center">
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.1" href="../news/jqnews.aspx">新闻动态</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.3" href="../news/jqannouncement.aspx">景区公告</a>
					</dd>
				</dl>
                <dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.2" href="../news/jqactivity.aspx">景区活动</a>
					</dd>
				</dl>
				
                <dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.3" href="#"> </a>
					</dd>
				</dl>

			</div>
			</div>
		</div>
		<div id="wechat" class="nav-down-menu menu-3 menu-1" style="display: none;" _t_nav="wechat">
			<div class="navigation-down-inner">
			<div class="inner-center">
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.1" href="../jqjs/jqjs_jqgk.aspx">景区概况</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.2" href="../jqjs/jqjs_gshm.aspx">龟山汉墓</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.3" href="../jqjs/jqjs_szg.aspx">圣旨博物馆</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.1" href="../jqjs/jqjs_dsy.aspx">点石园</a>
					</dd>
				</dl>
			</div>
			
			</div>
		</div>
		<div id="solution" class="nav-down-menu menu-3 menu-1" style="display: none;" _t_nav="solution">
			<div class="navigation-down-inner">
			<div class="inner-center">
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.1" href="../gshm3.0.html">虚拟旅游</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.2" href="../jqwh/lygl.aspx">旅游攻略</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.3" href="../ykfw/interaction.aspx">游客互动</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.solution.3" href="../ykfw/onlinesurvey.aspx">在线调查</a>
					</dd>
				</dl>
			</div>
			</div>
		</div>
		<div id="cooperate" class="nav-down-menu menu-3 menu-1" style="display: none;" _t_nav="cooperate">
			<div class="navigation-down-inner">
			<div class="inner-center">
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.support.1" href="../ykfw/fwzn.aspx">服务指南</a>
					</dd>
				</dl>
				
				<dl>
				    <dd>
						<a class="link" hotrep="hp.header.support.3" href="../ykfw/ydzx.aspx?id=1001">票务信息</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.support.1" href="../ykfw/map.aspx">景区地图</a>
					</dd>
				</dl>
				<dl>
				<dd>
						<a class="link" hotrep="hp.header.support.1" href="../ykfw/surrounding.aspx">周边环境</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.support.2" href="../ykfw/souvenir.aspx">纪念品</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a class="link" hotrep="hp.header.support.3" href="../ykfw/complain.aspx">旅游投诉</a>
					</dd>
				</dl>
			</div>
			</div>
		</div>
		<div id="support" class="nav-down-menu menu-3 menu-1" style="display: none;" _t_nav="support">
			<div class="navigation-down-inner">
			<div class="inner-center">
				<dl>
					<dd>
						<a hotrep="hp.header.partner.1" href="../jqwh/shipin.aspx">精彩视频</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a hotrep="hp.header.partner.2" href="../jqwh/baodao.aspx">媒体关注</a>
					</dd>
				</dl>
				<dl>
					<dd>
						<a hotrep="hp.header.partner.3" href="../jqwh/wenzhang.aspx">研究文章</a>
					</dd>
				</dl>
			</div>
			</div>
		</div>
	</div>
</div>
</div>
    
        <!--利用AJAX调用Web服务-->
        <asp:ScriptManager ID="scriptManager" runat="server">
            <Services>
                <asp:ServiceReference Path="~/ykfw/ykfwService.asmx" />
            </Services>
        </asp:ScriptManager>

        <!--浮动窗口动画脚本-->
        <script src="../uploadify/jquery.uploadify.min.js" type="text/javascript"></script>
        <script type="text/javascript">
            // 检测登录结果
            function checkLoginResult(result) {
                $("body").css("overflow-y", "hidden");
                $("#bkgnd").fadeIn(700);
                result ? $("#upload").slideDown(1000) : $("#login").slideDown(1000);
            }

            // 注册结果
            function doRegisterResult(result) {
                result ? (alert("注册账号成功！"), $("#register").slideUp(700), 
                    $("#login").fadeIn(1000)) : alert("验证码错误，或服务器异常！");
            }

            // 登录结果
            function doLoginResult(result) {
                result ? ($("#login").slideUp(700), $("#bkgnd").fadeOut(1000),
                    $("body").css("overflow-y", "auto")) : alert("登录失败！");
            }

            // 上传结果
            function uploadNoteResult(result) {
                result ? ($("#upload").slideUp(700), $("#bkgnd").fadeOut(1000),
                    $("body").css("overflow-y", "auto")) : alert("上传失败！");
            }

            // 生成验证码结果
            function makeCheckCodeResult(result) {
                result ? alert("验证码已通过短信发送至您的手机！") : alert("服务器异常！");
            }

            // 文本转html
            function text2html(context) {
                if (context != null) {
                    context = context.replace(new RegExp(" ", "gm"), "&nbsp;");
                    context = context.replace(new RegExp("<", "gm"), "&lt;");
                    context = context.replace(new RegExp(">", "gm"), "&gt;");
                    context = context.replace(new RegExp("\n", "gm"), "<br />");
                }
                return context;
            }

            $(document).ready(function () {
                // 上传框
                $("#file").uploadify({
                    fileTypeDesc: '图像文件',
                    fileTypeExts: '*.jpg;*.jpeg;*.png;*.bmp',
                    fileSizeLimit: '1MB',
                    buttonText: '选取图片',
                    swf : '../uploadify/uploadify.swf',
                    uploader: './uploadHandler.ashx',
                    onUploadSuccess: function (file, data, response) {
                        ykfwService.addImage('' + data);
                    }
                });
                // 点击相框
                $(".photo").click(function () {
                    $("body").css("overflow-y", "hidden");
                    $("#photoshow").find("span").html($(this).find("span").eq(2).html());
                    $("#bkgnd").fadeIn(700);
                    $("#photoshow").slideDown(1000);
                    // 删掉之前的轮播器
                    $(".flexslider").remove();
                    // 创建轮播器
                    var flexSlider = $("<div></div>");
                    flexSlider.addClass("flexslider");
                    $("#slider").append(flexSlider);
                    flexSlider.html($(this).find("div").eq(0).html());
                    flexSlider.flexslider({
                        animation: "slide",
                        animationLoop: true,
                        slideshowSpeed: 2000
                    });
                });
                // 点击关闭按钮
                $(".close").click(function () {
                    if ($(this).attr("id") == "c1")
                        $("#register").slideUp(700);
                    else if ($(this).attr("id") === "c2")
                        $("#login").slideUp(700);
                    else if ($(this).attr("id") === "c3")
                        $("#upload").slideUp(700);
                    else if ($(this).attr("id") === "c4")
                        $("#photoshow").slideUp(700);
                    $("#bkgnd").fadeOut(1000);
                    $("body").css("overflow-y", "auto");
                });
                // 点击我要传图按钮
                $("#btnSend").click(function () {
                    ykfwService.checkLogin(checkLoginResult);
                });
                // 点击注册新账号按钮
                $("#btnRegNew").click(function () {
                    $("#login").fadeOut(700);
                    $("#register").slideDown(1000);
                });
                // 点击注册按钮
                $("#btnReg").click(function () {
                    if ($("#username1").val() === "" || $("#password1").val() === "" || $("#checkcode").val() === "")
                        alert("用户名、密码和验证码不可为空");
                    else
                        ykfwService.doRegister($("#username1").val(), $("#password1").val(), $("#checkcode").val(), doRegisterResult);
                });
                // 点击获取验证码按钮
                $("#getcheckcode").click(function () {
                    if ($("#cellphone").val() === "")
                        alert("手机号不可为空");
                    else
                        ykfwService.makeCheckCode($("#cellphone").val(), makeCheckCodeResult);
                });
                // 点击登录按钮
                $("#btnLogin").click(function () {
                    if ($("#username").val() === "" || $("#password").val() === "")
                        alert("用户名和密码不可为空");
                    else
                        ykfwService.doLogin($("#username").val(), $("#password").val(), doLoginResult);
                });
                // 点击上传按钮
                $("#btnUpload").click(function () {
                    if ($("#context").val() === "")
                        alert("游记内容不可为空！");
                    else
                        ykfwService.uploadNote(text2html($("#context").val()), uploadNoteResult);
                });
                // 点击搜索按钮
                $("#btnSearch").click(function () {
                    location.href = "interaction.aspx?searchusr=" + $("#searchusr").val();
                });
            });
        </script>

         <div class="middle" style="margin-top:30px;">
            <div class="midtop">
                <div class="midtopbar"style="background:rgb(205,92,92);"  >
                    <a href="../index.aspx" style="height:35px; line-height:35px;float:left;">&nbsp;&nbsp;返回首页</a>
                    <a href="interaction.aspx" style="height:35px; line-height:35px;float:left;">&nbsp;&nbsp;游客互动</a>
                    <input id="btnSend" type="button" value="我要传图" class="Button" style="left:210px;" />
            <label class="Input" style="left:700px;width:120px;"><input type="radio" name="opt" checked="checked" onclick="location.href='interaction.aspx';" />按时间排序</label>
            <label class="Input" style="left:700px;top:23px;width:100px;*top:-10px;*left:742px;"><input type="radio" name="opt" onclick="$('#search').fadeIn(700);" />搜索作者</label>
                </div>
                      
             </div>      
         </div>

        <!--注册、传图、登录、图片展示-->
        <div id="bkgnd" style="display:none;position:fixed;top:0px;width:100%;height:100%;z-index:999;background-image:url(../img/imgshowbg.png);">
            <!--注册框-->
            <div id="register" class="Window" runat="server" style="display:none;">
                <div style="position:absolute;top:5px;width:100%;color:white;font-size:30px;font-weight:bold;text-align:center;">注册</div>
                <div id="c1" class="close"></div>
                <div style="position:absolute;top:85px;left:70px;">用户名:</div>
                <input id="username1" type="text" class="Input" style="top:80px;left:150px;" />
                <div style="position:absolute;top:135px;left:70px;">密码: </div>
                <input id="password1" type="password" class="Input" style="top:130px;left:150px;" />
                <div style="position:absolute;top:185px;left:70px;">手机号: </div>
                <input id="cellphone" type="text" class="Input" style="top:180px;left:150px;" />
                <input id="getcheckcode" type="button" class="Button" value="获取验证码" style="top:177px;left:365px;height:40px;width:100px;"/>
                <div style="position:absolute;top:235px;left:70px;">验证码: </div>
                <input id="checkcode" type="text" class="Input" style="top:230px;left:150px;" />
                <input id="btnReg" type="button" class="Button" value="注册" style="top:310px;left:175px;"/>
            </div>

            <!--登录框-->
            <div id="login" class="Window" runat="server" style="display:none;">
                <div style="position:absolute;top:5px;width:100%;color:white;font-size:30px;font-weight:bold;text-align:center;">登录</div>
                <div id="c2" class="close"></div>
                <div style="position:absolute;top:135px;left:70px;">用户名:</div>
                <input id="username" type="text" class="Input" style="top:130px;left:150px;" />
                <div style="position:absolute;top:215px;left:70px;">密码: </div>
                <input id="password" type="password" class="Input" style="top:210px;left:150px;" />
                <input id="btnLogin" type="button" class="Button" value="登录" style="top:280px;left:90px;"/>
                <input id="btnRegNew" type="button" class="Button" value="注册新账号" style="top:280px;left:260px;"/>
            </div>

            <!--上传框-->
            <div id="upload" class="Window" runat="server" style="display:none;height:600px;margin-top:80px;">
                <div style="position:absolute;top:5px;width:100%;color:white;font-size:30px;font-weight:bold;text-align:center;">上传</div>
                <div id="c3" class="close"></div>
                <div style="position:absolute;top:135px;left:54px;">请选择照片:</div>
                <div style="position:absolute;top:130px;left:150px;">
                    <input id="file" type="file" style="z-index:999" />
                </div>
                <div style="position:absolute;top:215px;left:70px;">您的游记:</div>
                <textarea id="context" class="Input" style="top:210px;left:150px;width:300px;height:300px;"></textarea>
                <input id="btnUpload" type="button" value="上传" class="Button" style="top:530px;left:175px;"/>
            </div>

            <!--图片展示框-->
            <div id="photoshow" class="Window" runat="server" style="display:none;height:600px;margin-top:80px;">
                <div style="position:absolute;top:5px;width:100%;color:white;font-size:30px;font-weight:bold;text-align:center;">游记</div>
                <div id="c4" class="close"></div>
                <div id="slider" style="margin-top:30px;">
                    <!-- <div class="flexslider" style="margin-top:30px;">
                        轮播器图片
                    </div> -->
                </div>
                <div style="margin-left:25px;margin-top:10px;width:450px;height:200px;overflow-y:auto;">
                    <span style="font-family:'Microsoft YaHei';font-size:14px;"></span>
                </div>
            </div>
        </div>

        <div style="width:1000px;height:auto;margin-right:auto;margin-left:auto;margin-top:-5px;background-color:lightgray;">

        <!--顶栏按钮-->

            
            <div id="search" style="display:none;position:absolute;left:1000px;height:70px;width:180px;margin-top:-36px;z-index:2;">
                <input id="searchusr" class="Input" type="text" style="top:10px;height:25px;width:100px;"/>
                <input id="btnSearch" type="button" value="Go" class="ButtonGo" style="top:5px;height:40px;width:50px;left:120px;" />
            </div>


            <center>
                <table class="tbl" cellspacing="5">
                    <%=getTblData() %>
                </table>
            </center>
        </div>
        <div class="footdiv">
          <div class="foot22 wow fadeInRight">
            <div class="foot22_1" >
                    <br />[<a style="color:rgb(177, 126, 21);margin-top:50px;"  href="http://www.byunit.com/" target="_blank">佰云科技&智慧旅游</a>]提供技术支持 |        徐州龟山汉墓管理处版权所有    Copyright&copy;2015 All Rights Reserved.
                <p id="back-to-top" class="wow bounceInUp" style=" margin-top:20px;"><a href="#top"><span></span></a></p>
            
           </div>
         </div>
       </div>
</div>
      
</form>  

</body>
</html>
