program p1293; 
const maxL = 1000000; 
type node = record 
ch:char; 
l,r:longint; 
end; 
var s:ansistring; 
List:array[1..maxL] of node; 
T:longint; 

function GetLevel(x:char):longint; 
begin 
case x of 
'*','/':exit(3); 
'-':exit(1); 
'+':exit(1); 
end; 
end; 

procedure work(var s:ansistring); 
var now:longint; 
begin 
inc(T); 
now:=T; 
List[now].ch:=s[length(s)]; 
delete(s,length(s),1); 
if List[now].ch in ['+','-','*','/'] then begin 
List[now].R:=T+1; 
work(s); 
List[now].L:=T+1; 
work(s); 
end 
else begin 
List[now].R:=-1; 
List[now].L:=-1; 
end; 
end; 

function outp(x:longint;level:longint;c:longint;d:char):ansistring; 
var tmps:ansistring; 
nowLevel:longint; 
begin 
if List[x].ch in ['+','-','*','/'] then begin 
nowLevel:=GetLevel(List[x].ch); 
tmps:=outp(List[x].L,nowLevel,1,List[x].ch)+List[x].ch+outp(List[x].R,nowLevel,2,List[x].ch); 
if nowLevel<level then begin 
tmps:='('+tmps+')'; 
end; 
if (nowLevel=level) and (c=2) and 
(((d='/')and(List[x].ch='*'))or((d='+')and(List[x].ch='-'))or((List[x].ch='/')and(d='/'))) then begin 
tmps:='('+tmps+')'; 
end; 
end 
else 
tmps:=List[x].ch; 
outp:=tmps; 
end; 

begin 
readln(s); 
t:=0; 
work(s); 
writeln(outp(1,GetLevel(List[1].ch),1,List[1].ch)); 
end.