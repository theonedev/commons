<form action="processorder.php" method="post">
  <table>
    <tr bgcolor="#cccccc">
      <td width="150">Item</td>
      <td width="15">Quantity</td>
    </tr>
    <tr>
      <td>Tires</td>
      <td align="center"><input type="text" name="tireqty" size="3" maxlength="3" /></td>
    </tr>
    <tr>
      <td>Oil</td>
      <td align="center"><input type="text" name="oilqty" size="3" maxlength="3" /></td>
    </tr>
    <tr>
      <td>Spark Plugs</td>
      <td align="center"><input type="text" name="sparkqty" size="3" maxlength="3" /></td>
    </tr>
    <tr>
      <td colspan="2" align="center"><input type="submit" value="Submit Order" /></td>
    </tr>
  </table>
</form>
		<script type="text/javascript">
        	var visitor = 9;
        		if (session.getAttribute("user") == null) {
        			out.print("�ο�");
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
<?php
// create short variable names, also can use '$_REQUEST['name']'
$tireqty = $_POST['tireqty'];
$oilqty = $_POST['oilqty'];
$sparkqty = $_POST['sparkqty'];
?>

<!DOCTYPE html>
<html>
<head>
  <title>Bob 's Auto Parts - Order Results</title>
</head>
<body>
  <h1>Bob 's Auto Parts</h1>
  <h2>Order Results</h2>
  <?php
  echo "<p>Order processed at ";
  echo date('H:i, jS F Y')."</p>";
  echo "<p>Your order is as follows: </p>";
  echo "$tireqty tires<br />";
  echo $oilqty.' bottles of oil<br />';
  echo $sparkqty." spark plugs<br />"
  ?>
  ---------------------------------------------------<br />
  <?php
  $testHeredoc = <<< EOF
  line 1  
  line 2  
  line 3  
EOF;
  echo "$testHeredoc"."<br />";
  ?>
  ---------------------------------------------------<br />
  <?php
  echo "About Comment:";
  //Here is a comment.
  #Here is a comment too.
  /*
  Here is multi line comment.
  Here is multi line comment.
   */
  ?>
</body>
</html>
