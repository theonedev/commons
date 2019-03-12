#!/usr/bin/perl
use LWP::Simple qw/get/;

my %pages;
print "Processing the index.\n";
$_ = get("http://www.yifan100.com/dir/15136/");
while(m#<a target="_blank" href="/article/(.*?)\.html" title="(.*?)" >#g){
    $pages{$1}=$2;
}
for(keys %pages){
    my ($l, $f) = ("http://www.yifan100.com/article/$_.html", "$_.txt");
    open F, ">$f";
    print "Processing $l.\n";
    if(get($l) =~ m#<div class="artcontent">(.*)<div id="zhanwei">#s){
        $_ = $1;
        s#<br>#\n#g;
        s#<.*?>##gs;
        s#^\s+##g;
        print "Writing to $f.\n";
        print F;
    }
    close F;
}


use Mail::POP3Client;
use MIME::Parser;
  
my $U = 'User.Name@gmail.com';
my $P = 'uSeR.pAsSwORd';
my $X = new MIME::Parser;
$X -> output_dir('C:\\download');  #directory to save attachment
  
my $G = Mail::POP3Client -> new (
      USER    => $U,
      PASSWORD  => $P,
      HOST    => 'pop.gmail.com',
      PORT    => 995,
      USESSL   => 'true') or die "Can't Connect The Server.\n";
  
for $i (1 .. $G->Count())
{
  my $C = $G->HeadAndBody($i);
  my $R = $X->parse_data($C);
}
$G->Close();


#!/usr/bin/perl
 use strict;
 use warnings;
 use Encode qw/from_to/; 
  
 my $path = "e:/CSS Design";
 my $filecount = 0; 
  
 sub parse_env {    
     my $path = $_[0]; #或者使用 my($path) = @_; @_类似javascript中的arguments
     my $subpath;
     my $handle; 
  
     if (-d $path) {#当前路径是否为一个目录
         if (opendir($handle, $path)) {
             while ($subpath = readdir($handle)) {
                 if (!($subpath =~ m/^\.$/) and !($subpath =~ m/^(\.\.)$/)) {
                     my $p = $path."/$subpath"; 
  
                     if (-d $p) {
                         parse_env($p);
                     } else {
                         ++$filecount;
                         print $p."\n";
                     }
                 }                
             }
             closedir($handle);            
         }
     } 
  
     return $filecount;
 } 
  
 my $count = parse_env $path;
 my $str = "文件总数：".$count;
 from_to($str, "utf8", "gbk"); 
  
 print $str; 
