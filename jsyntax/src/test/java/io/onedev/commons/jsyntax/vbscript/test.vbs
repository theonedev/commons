Option Explicit 
On Error Resume Next 

'生成列表的文件类型 
Const sListFileType = "wmv,rm,wma" 

'文件所在的相对路径 
Const sShowPath="." 

'排序类型的常量定义 
Const iOrderFieldFileName = 0 
Const iOrderFieldFileExt = 1 
Const iOrderFieldFileSize = 2 
Const iOrderFieldFileType = 3 
Const iOrderFieldFileDate = 4 

'排序顺逆的常量定义 
const iOrderAsc = 0 
const iOrderDesc = 1 

'生成列表的文件数量 
const iShowCount = 20 


'显示的日期格式函数 
Function Cndate2(date1,intDateStyle) 
dim strdate,dDate1 
strdate=cstr(date1) 
If Isdate(strdate) Then 
If Left(cstr(strdate),1)="0" Then 
dDate1=Cdate("20"+cstr(strdate)) 
else 
dDate1=Cdate(strdate) 
End If 
Else 
dDate1=Now() 
End If 
Select case intDateStyle 
Case 1: 
Cndate2 = Cstr(Year(dDate1))+"-"+Cstr(Month(dDate1))+"-"+Cstr(Day(dDate1)) 
Case 2: 
Cndate2 = Cstr(Month(dDate1))+"-"+Cstr(Day(dDate1)) 
Case 3: 
Cndate2 = Cstr(Month(dDate1))+"月"+Cstr(Day(dDate1))+"日" 
Case 4: 
Cndate2 = Cstr(year(dDate1))+"年"+ Cstr(Month(dDate1))+"月"+Cstr(Day(dDate1))+"日" 
End Select 
End Function 

Function ListFile(strFiletype,intCompare,intOrder,intShowCount) 
Dim sListFile 
Dim fso, f, f1, fc, s,ftype,fcount,i,j,k 
Dim t1,t2,t3,t4,t5 
Dim iMonth,iDay 
sListFile = "" 
Set fso = CreateObject("Scripting.FileSystemObject") 
Set f = fso.GetFolder(sShowPath) 
Set fc = f.Files 
fcount = fc.count 
redim arrFiles(fcount,5) 
redim arrFiles2(fcount,5) 
i=0 
'排序 
For Each f1 in fc 
ftype = right(f1.name,len(f1.name)-instrrev(f1.name,".")) 
arrFiles(i,0) = f1.name 
arrFiles(i,1) = ftype 
arrFiles(i,2) = f1.size 
arrFiles(i,3) = f1.type 
arrFiles(i,4) = f1.DateLastModified 
i=i+1 
Next 
For i=0 to fcount-1 
for j=i+1 to fcount-1 
select Case intCompare 
Case iOrderFieldFileName,iOrderFieldFileExt,iOrderFieldFileType: 
If arrFiles(i,intCompare)>arrFiles(j,intCompare) then 
t1 = arrFiles(i,0) 
t2 = arrFiles(i,1) 
t3 = arrFiles(i,2) 
t4 = arrFiles(i,3) 
t5 = arrFiles(i,4) 

arrFiles(i,0) = arrFiles(j,0) 
arrFiles(i,1) = arrFiles(j,1) 
arrFiles(i,2) = arrFiles(j,2) 
arrFiles(i,3) = arrFiles(j,3) 
arrFiles(i,4) = arrFiles(j,4) 

arrFiles(j,0) = t1 
arrFiles(j,1) = t2 
arrFiles(j,2) = t3 
arrFiles(j,3) = t4 
arrFiles(j,4) = t5 
end if 
Case iOrderFieldFileSize: 
If cdbl(arrFiles(i,intCompare))>cdbl(arrFiles(j,intCompare)) then 
t1 = arrFiles(i,0) 
t2 = arrFiles(i,1) 
t3 = arrFiles(i,2) 
t4 = arrFiles(i,3) 
t5 = arrFiles(i,4) 

arrFiles(i,0) = arrFiles(j,0) 
arrFiles(i,1) = arrFiles(j,1) 
arrFiles(i,2) = arrFiles(j,2) 
arrFiles(i,3) = arrFiles(j,3) 
arrFiles(i,4) = arrFiles(j,4) 

arrFiles(j,0) = t1 
arrFiles(j,1) = t2 
arrFiles(j,2) = t3 
arrFiles(j,3) = t4 
arrFiles(j,4) = t5 
end if 
Case iOrderFieldFileDate: 
If Cdate(arrFiles(i,intCompare))>Cdate(arrFiles(j,intCompare)) then 
t1 = arrFiles(i,0) 
t2 = arrFiles(i,1) 
t3 = arrFiles(i,2) 
t4 = arrFiles(i,3) 
t5 = arrFiles(i,4) 

arrFiles(i,0) = arrFiles(j,0) 
arrFiles(i,1) = arrFiles(j,1) 
arrFiles(i,2) = arrFiles(j,2) 
arrFiles(i,3) = arrFiles(j,3) 
arrFiles(i,4) = arrFiles(j,4) 

arrFiles(j,0) = t1 
arrFiles(j,1) = t2 
arrFiles(j,2) = t3 
arrFiles(j,3) = t4 
arrFiles(j,4) = t5 
end if 
End Select 
next 
next 
'生成列表 
sListFile = sListFile + ("<table cellpadding=0 cellspacing=0 width=100% align=center class=""PageListTable"" style=""BEHAVIOR: url(images/sort2.htc); "">") 
sListFile = sListFile + ("<THEAD><Tr class=PageListTitleTr><Td class=PageListTitleTd>") 
sListFile = sListFile + ("名称") 
sListFile = sListFile + ("</td><Td class=PageListTitleTd>") 
sListFile = sListFile + ("媒体") 
sListFile = sListFile + ("</td><Td class=PageListTitleTd>") 
sListFile = sListFile + ("大小") 
sListFile = sListFile + ("</td><Td class=PageListTitleTd>") 
sListFile = sListFile + ("类型") 
sListFile = sListFile + ("</td><Td class=PageListTitleTd ID=updatetime>") 
sListFile = sListFile + ("更新时间") 
sListFile = sListFile + ("</td></Tr></THEAD>") 
dim iLoopStart,iLoofEnd,iLoopStep 
If intOrder = 0 then 
iLoopStart = 0 
iLoofEnd = fcount-1 
iLoopStep = 1 
Else 
iLoopStart = fcount-1 
iLoofEnd = 0 
iLoopStep = -1 
End if 
dim iCount,sTDStyleClass 
iCount = 1 
For j=iLoopStart to iLoofEnd Step iLoopStep 
If instr(strFiletype,arrFiles(j,1))>0 and iCount<=intShowCount then 
sTDStyleClass = "PageListTd"+Cstr((iCount mod 2)+1) 
sListFile = sListFile + ("<Tr class=PageListTr><Td class="+sTDStyleClass+">") 
sListFile = sListFile + ("<img src=images/"+arrFiles(j,1)+".gif align=absbottom><img src=b.gif width=2 height=0><a href=" & sShowPath & "/" & CStr(arrFiles(j,0)) &">" & arrFiles(j,0) &"</a>") 
If datediff("h",arrFiles(j,4),now)<=24 then 
sListFile = sListFile + "<img src=images/new.gif align=absmiddle>" 
end if 
sListFile = sListFile + "</td><Td class="+sTDStyleClass+">" 
sListFile = sListFile + ("<a href=" & sShowPath & "/" & CStr(arrFiles(j,0)) &">") 
'根据文件名规则,生成中文提示 
select case left(arrFiles(j,0),3) 
case "sc2": 
sListFile = sListFile + "<font color=#AA0000>四川卫视 " 
case "sd2": 
sListFile = sListFile + "<font color=#00AA00>山东卫视 " 
case "gd2": 
sListFile = sListFile + "<font color=#0000AA>广东卫视 " 
case "gx2": 
sListFile = sListFile + "<font color=#AAAA00>广西卫视 " 
end select 
'日期显示 
If isnumeric(left(right(arrFiles(j,0),8),2)) then 
iMonth = cint(left(right(arrFiles(j,0),8),2)) 
iDay = cint(left(right(arrFiles(j,0),6),2)) 
sListFile = sListFile + cstr(iMonth)+"月" + cstr(iDay)+"日" 
sListFile = sListFile + ("</a></td><Td class="+sTDStyleClass+" align=right>") 
Else 
response.write arrFiles(j,0) 
end if 
If arrFiles(j,2)>1024*1024 then 
sListFile = sListFile + cstr(round(arrFiles(j,2)/1024/1024)) 
sListFile = sListFile + ("MB") 
else 
sListFile = sListFile + cstr(round(arrFiles(j,2)/1024)) 
sListFile = sListFile + ("KB") 
end if 
sListFile = sListFile + (" </td>") 
sListFile = sListFile + ("<Td class="+sTDStyleClass+">") 
sListFile = sListFile + cstr(arrFiles(j,3)) 
sListFile = sListFile + ("</td>") 
sListFile = sListFile + ("<Td class="+sTDStyleClass+">") 
sListFile = sListFile + (Cndate2(arrFiles(j,4),4)) 
sListFile = sListFile + ("</td>") 
sListFile = sListFile + ("</Tr>") 
iCount = iCount+1 
end if 
next 
sListFile = sListFile + "</table>" 
ListFile = sListFile 
End Function 

'生成调用文件的过程 
Sub ShowFileListContent() 
Dim tUpdatetime,sUpdateContent 

Dim fso,f,f_js,f_js_write 
Set fso = CreateObject("Scripting.FileSystemObject") 
Set f = fso.GetFolder(sShowPath) 
Set f_js = fso.GetFile("list.js") 

'比较调用文件与文件夹的最后修改时间 
If f.DateLastModified<>f_js.DateLastModified then 
sUpdateContent = ListFile(sListFileType,iOrderFieldFileDate,iOrderDesc,iShowCount) 
Set f_js_write = fso.CreateTextFile("list.js", True) 
'JS调用就加上下面这对document.write 
' f_js_write.Write ("document.write('") 
f_js_write.Write (sUpdateContent) 
' f_js_write.Write ("')") 
f_js_write.Close 
End If 
End Sub 

Call ShowFileListContent() 
