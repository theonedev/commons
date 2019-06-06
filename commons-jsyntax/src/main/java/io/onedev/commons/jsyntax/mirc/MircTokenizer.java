package io.onedev.commons.jsyntax.mirc;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class MircTokenizer extends AbstractTokenizer<MircTokenizer.State> {
	static Set<String> parseWords(String str) {
		Set<String> s = Sets.newHashSet();
		String[] arr = str.split(" ");
		for (String o : arr) {
			s.add(o);
		}
		return s;
	}

	static final Set<String> specials = parseWords("$! $$ $& $? $+ $abook $abs $active $activecid "
			+ "$activewid $address $addtok $agent $agentname $agentstat $agentver "
			+ "$alias $and $anick $ansi2mirc $aop $appactive $appstate $asc $asctime "
			+ "$asin $atan $avoice $away $awaymsg $awaytime $banmask $base $bfind "
			+ "$binoff $biton $bnick $bvar $bytes $calc $cb $cd $ceil $chan $chanmodes "
			+ "$chantypes $chat $chr $cid $clevel $click $cmdbox $cmdline $cnick $color "
			+ "$com $comcall $comchan $comerr $compact $compress $comval $cos $count "
			+ "$cr $crc $creq $crlf $ctime $ctimer $ctrlenter $date $day $daylight "
			+ "$dbuh $dbuw $dccignore $dccport $dde $ddename $debug $decode $decompress "
			+ "$deltok $devent $dialog $did $didreg $didtok $didwm $disk $dlevel $dll "
			+ "$dllcall $dname $dns $duration $ebeeps $editbox $emailaddr $encode $error "
			+ "$eval $event $exist $feof $ferr $fgetc $file $filename $filtered $finddir "
			+ "$finddirn $findfile $findfilen $findtok $fline $floor $fopen $fread $fserve "
			+ "$fulladdress $fulldate $fullname $fullscreen $get $getdir $getdot $gettok $gmt "
			+ "$group $halted $hash $height $hfind $hget $highlight $hnick $hotline "
			+ "$hotlinepos $ial $ialchan $ibl $idle $iel $ifmatch $ignore $iif $iil "
			+ "$inelipse $ini $inmidi $inpaste $inpoly $input $inrect $inroundrect "
			+ "$insong $instok $int $inwave $ip $isalias $isbit $isdde $isdir $isfile "
			+ "$isid $islower $istok $isupper $keychar $keyrpt $keyval $knick $lactive "
			+ "$lactivecid $lactivewid $left $len $level $lf $line $lines $link $lock "
			+ "$lock $locked $log $logstamp $logstampfmt $longfn $longip $lower $ltimer "
			+ "$maddress $mask $matchkey $matchtok $md5 $me $menu $menubar $menucontext "
			+ "$menutype $mid $middir $mircdir $mircexe $mircini $mklogfn $mnick $mode "
			+ "$modefirst $modelast $modespl $mouse $msfile $network $newnick $nick $nofile "
			+ "$nopath $noqt $not $notags $notify $null $numeric $numok $oline $onpoly "
			+ "$opnick $or $ord $os $passivedcc $pic $play $pnick $port $portable $portfree "
			+ "$pos $prefix $prop $protect $puttok $qt $query $rand $r $rawmsg $read $readomo "
			+ "$readn $regex $regml $regsub $regsubex $remove $remtok $replace $replacex "
			+ "$reptok $result $rgb $right $round $scid $scon $script $scriptdir $scriptline "
			+ "$sdir $send $server $serverip $sfile $sha1 $shortfn $show $signal $sin "
			+ "$site $sline $snick $snicks $snotify $sock $sockbr $sockerr $sockname "
			+ "$sorttok $sound $sqrt $ssl $sreq $sslready $status $strip $str $stripped "
			+ "$syle $submenu $switchbar $tan $target $ticks $time $timer $timestamp "
			+ "$timestampfmt $timezone $tip $titlebar $toolbar $treebar $trust $ulevel "
			+ "$ulist $upper $uptime $url $usermode $v1 $v2 $var $vcmd $vcmdstat $vcmdver "
			+ "$version $vnick $vol $wid $width $wildsite $wildtok $window $wrap $xor");
	static final Set<String> keywords = parseWords(
			"abook ajinvite alias aline ame amsg anick aop auser autojoin avoice "
					+ "away background ban bcopy beep bread break breplace bset btrunc bunset bwrite "
					+ "channel clear clearall cline clipboard close cnick color comclose comopen "
					+ "comreg continue copy creq ctcpreply ctcps dcc dccserver dde ddeserver "
					+ "debug dec describe dialog did didtok disable disconnect dlevel dline dll "
					+ "dns dqwindow drawcopy drawdot drawfill drawline drawpic drawrect drawreplace "
					+ "drawrot drawsave drawscroll drawtext ebeeps echo editbox emailaddr enable "
					+ "events exit fclose filter findtext finger firewall flash flist flood flush "
					+ "flushini font fopen fseek fsend fserve fullname fwrite ghide gload gmove "
					+ "gopts goto gplay gpoint gqreq groups gshow gsize gstop gtalk gunload hadd "
					+ "halt haltdef hdec hdel help hfree hinc hload hmake hop hsave ial ialclear "
					+ "ialmark identd if ignore iline inc invite iuser join kick linesep links list "
					+ "load loadbuf localinfo log mdi me menubar mkdir mnick mode msg nick noop notice "
					+ "notify omsg onotice part partall pdcc perform play playctrl pop protect pvoice "
					+ "qme qmsg query queryn quit raw reload remini remote remove rename renwin "
					+ "reseterror resetidle return rlevel rline rmdir run ruser save savebuf saveini "
					+ "say scid scon server set showmirc signam sline sockaccept sockclose socklist "
					+ "socklisten sockmark sockopen sockpause sockread sockrename sockudp sockwrite "
					+ "sound speak splay sreq strip switchbar timer timestamp titlebar tnick tokenize "
					+ "toolbar topic tray treebar ulist unload unset unsetall updatenl url uwho "
					+ "var vcadd vcmd vcrem vol while whois window winhelp write writeint if isalnum "
					+ "isalpha isaop isavoice isban ischan ishop isignore isin isincs isletter islower "
					+ "isnotify isnum ison isop isprotect isreg isupper isvoice iswm iswmcs "
					+ "elseif else goto menu nicklist status title icon size option text edit "
					+ "button check radio box scroll list combo link tab item");
	static final Set<String> functions = parseWords("if elseif else and not or eq ne in ni for foreach while switch");
	static Pattern isOperatorChar = Pattern.compile("[+\\-*&%=<>!?^\\/\\|]");

	static class State {
		Processor tokenize;
		boolean beforeParams;
		boolean inParams;

		public State(Processor tokenize, boolean beforeParams, boolean inParams) {
			this.tokenize = tokenize;
			this.beforeParams = beforeParams;
			this.inParams = inParams;
		}
	}

	static interface Processor {
		String processor(StringStream stream, State state);
	}

	static final Pattern pattern[] = new Pattern[9];
	static {
		pattern[0] = Pattern.compile("[\\[\\]{}\\(\\),\\.]");
		pattern[1] = Pattern.compile("\\d");
		pattern[2] = Pattern.compile("[\\w\\.]");
		pattern[3] = Pattern.compile(".");
		pattern[4] = Pattern.compile(" *\\( *\\(");
		pattern[5] = Pattern.compile("'");
		pattern[6] = Pattern.compile("[$_a-z0-9A-Z\\.:]");
		pattern[7] = Pattern.compile("[^,\\s()]");
		pattern[8] = Pattern.compile("[\\w\\$_{}]");
	}

	static String chain(StringStream stream, State state, Processor f) {
		state.tokenize = f;
		return f.processor(stream, state);
	}

	class tokenBase implements Processor {
		@Override
		public String processor(StringStream stream, State state) {
			boolean beforeParams = state.beforeParams;
			state.beforeParams = false;
			String ch = stream.next();
			if (pattern[0].matcher(ch).matches()) {
				if (ch.equals("(") && beforeParams)
					state.inParams = true;
				else if (ch.equals(")"))
					state.inParams = false;
				return "";
			} else if (pattern[1].matcher(ch).matches()) {
				stream.eatWhile(pattern[2]);
				return "number";
			} else if (ch.equals("\\")) {
				stream.eat("\\");
				stream.eat(pattern[3]);
				return "number";
			} else if (ch.equals("/") && !stream.eat("*").isEmpty()) {
				return chain(stream, state, new tokenComment());
			} else if (ch.equals(";") && !stream.match(pattern[4], false).isEmpty()) {
				return chain(stream, state, new tokenUnparsed());
			} else if (ch.equals(";") && !state.inParams) {
				stream.skipToEnd();
				return "comment";
			} else if (ch.equals("\"")) {
				stream.eat(pattern[5]);
				return "keyword";
			} else if (ch.equals("$")) {
				stream.eatWhile(pattern[6]);
				if (specials != null && specials.contains(stream.current().toLowerCase())) {
					return "keyword";
				} else {
					state.beforeParams = true;
					return "builtin";
				}
			} else if (ch.equals("%")) {
				stream.eatWhile(pattern[7]);
				state.beforeParams = true;
				return "string";
			} else if (isOperatorChar.matcher(ch).matches()) {
				stream.eatWhile(isOperatorChar);
				return "operator";
			} else {
				stream.eatWhile(pattern[8]);
				String word = stream.current().toLowerCase();
				if (keywords != null && keywords.contains(word))
					return "keyword";
				if (functions != null && functions.contains(word)) {
					state.beforeParams = true;
					return "keyword";
				}
				return "";
			}
		}
	}

	class tokenComment implements Processor {
		@Override
		public String processor(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch = "";
			while (!(ch = stream.next()).isEmpty()) {
				if (ch.equals("/") && maybeEnd) {
					state.tokenize = new tokenBase();
					break;
				}
				maybeEnd = (ch.equals("*"));
			}
			return "comment";
		}
	}

	class tokenUnparsed implements Processor {
		@Override
		public String processor(StringStream stream, State state) {
			int maybeEnd = 0;
			String ch = stream.next();
			while (!(ch = stream.next()).isEmpty()) {
				if (ch.equals(";") && maybeEnd == 2) {
					state.tokenize = new tokenBase();
					break;
				}
				if (ch.equals(")"))
					maybeEnd++;
				else if (!ch.equals(" "))
					maybeEnd = 0;
			}
			return "meta";
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "mirc");
	}

	@Override
	public State startState() {
		return new State(new tokenBase(), false, false);
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-mirc");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("mirc");
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.eatSpace())
			return "";
		return state.tokenize.processor(stream, state);
	}
}
