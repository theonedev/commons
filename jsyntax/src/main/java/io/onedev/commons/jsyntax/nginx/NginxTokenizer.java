package io.onedev.commons.jsyntax.nginx;

import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;


public class NginxTokenizer extends AbstractTokenizer<NginxTokenizer.State> {
	
	static final Set<String> keywords = words("break return rewrite set accept_mutex accept_mutex_delay access_log add_after_body add_before_body add_header addition_types aio alias allow ancient_browser ancient_browser_value auth_basic auth_basic_user_file auth_http auth_http_header auth_http_timeout autoindex autoindex_exact_size autoindex_localtime charset charset_types client_body_buffer_size client_body_in_file_only client_body_in_single_buffer client_body_temp_path client_body_timeout client_header_buffer_size client_header_timeout client_max_body_size connection_pool_size create_full_put_path daemon dav_access dav_methods debug_connection debug_points default_type degradation degrade deny devpoll_changes devpoll_events directio directio_alignment empty_gif env epoll_events error_log eventport_events expires fastcgi_bind fastcgi_buffer_size fastcgi_buffers fastcgi_busy_buffers_size fastcgi_cache fastcgi_cache_key fastcgi_cache_methods fastcgi_cache_min_uses fastcgi_cache_path fastcgi_cache_use_stale fastcgi_cache_valid fastcgi_catch_stderr fastcgi_connect_timeout fastcgi_hide_header fastcgi_ignore_client_abort fastcgi_ignore_headers fastcgi_index fastcgi_intercept_errors fastcgi_max_temp_file_size fastcgi_next_upstream fastcgi_param fastcgi_pass_header fastcgi_pass_request_body fastcgi_pass_request_headers fastcgi_read_timeout fastcgi_send_lowat fastcgi_send_timeout fastcgi_split_path_info fastcgi_store fastcgi_store_access fastcgi_temp_file_write_size fastcgi_temp_path fastcgi_upstream_fail_timeout fastcgi_upstream_max_fails flv geoip_city geoip_country google_perftools_profiles gzip gzip_buffers gzip_comp_level gzip_disable gzip_hash gzip_http_version gzip_min_length gzip_no_buffer gzip_proxied gzip_static gzip_types gzip_vary gzip_window if_modified_since ignore_invalid_headers image_filter image_filter_buffer image_filter_jpeg_quality image_filter_transparency imap_auth imap_capabilities imap_client_buffer index ip_hash keepalive_requests keepalive_timeout kqueue_changes kqueue_events large_client_header_buffers limit_conn limit_conn_log_level limit_rate limit_rate_after limit_req limit_req_log_level limit_req_zone limit_zone lingering_time lingering_timeout lock_file log_format log_not_found log_subrequest map_hash_bucket_size map_hash_max_size master_process memcached_bind memcached_buffer_size memcached_connect_timeout memcached_next_upstream memcached_read_timeout memcached_send_timeout memcached_upstream_fail_timeout memcached_upstream_max_fails merge_slashes min_delete_depth modern_browser modern_browser_value msie_padding msie_refresh multi_accept open_file_cache open_file_cache_errors open_file_cache_events open_file_cache_min_uses open_file_cache_valid open_log_file_cache output_buffers override_charset perl perl_modules perl_require perl_set pid pop3_auth pop3_capabilities port_in_redirect postpone_gzipping postpone_output protocol proxy proxy_bind proxy_buffer proxy_buffer_size proxy_buffering proxy_buffers proxy_busy_buffers_size proxy_cache proxy_cache_key proxy_cache_methods proxy_cache_min_uses proxy_cache_path proxy_cache_use_stale proxy_cache_valid proxy_connect_timeout proxy_headers_hash_bucket_size proxy_headers_hash_max_size proxy_hide_header proxy_ignore_client_abort proxy_ignore_headers proxy_intercept_errors proxy_max_temp_file_size proxy_method proxy_next_upstream proxy_pass_error_message proxy_pass_header proxy_pass_request_body proxy_pass_request_headers proxy_read_timeout proxy_redirect proxy_send_lowat proxy_send_timeout proxy_set_body proxy_set_header proxy_ssl_session_reuse proxy_store proxy_store_access proxy_temp_file_write_size proxy_temp_path proxy_timeout proxy_upstream_fail_timeout proxy_upstream_max_fails random_index read_ahead real_ip_header recursive_error_pages request_pool_size reset_timedout_connection resolver resolver_timeout rewrite_log rtsig_overflow_events rtsig_overflow_test rtsig_overflow_threshold rtsig_signo satisfy secure_link_secret send_lowat send_timeout sendfile sendfile_max_chunk server_name_in_redirect server_names_hash_bucket_size server_names_hash_max_size server_tokens set_real_ip_from smtp_auth smtp_capabilities smtp_client_buffer smtp_greeting_delay so_keepalive source_charset ssi ssi_ignore_recycled_buffers ssi_min_file_chunk ssi_silent_errors ssi_types ssi_value_length ssl ssl_certificate ssl_certificate_key ssl_ciphers ssl_client_certificate ssl_crl ssl_dhparam ssl_engine ssl_prefer_server_ciphers ssl_protocols ssl_session_cache ssl_session_timeout ssl_verify_client ssl_verify_depth starttls stub_status sub_filter sub_filter_once sub_filter_types tcp_nodelay tcp_nopush thread_stack_size timeout timer_resolution types_hash_bucket_size types_hash_max_size underscores_in_headers uninitialized_variable_warn use user userid userid_domain userid_expires userid_mark userid_name userid_p3p userid_path userid_service valid_referers variables_hash_bucket_size variables_hash_max_size worker_connections worker_cpu_affinity worker_priority worker_processes worker_rlimit_core worker_rlimit_nofile worker_rlimit_sigpending worker_threads working_directory xclient xml_entities xslt_stylesheet xslt_typesdrew@li229-23");
	static final Set<String> keywords_block  = words("http mail events server types location upstream charset_map limit_except if geo map");
	static final Set<String> keywords_important  = words("include root server server_name listen internal proxy_pass memcached_pass fastcgi_pass try_files");
	static class State{
		Processor tokenize;
		Stack<String> stack;
		String type;
		public State(Processor tokenize,Stack<String> stack,String type) {
			this.tokenize = tokenize;
			this.stack = stack;
			this.type = type;
		}
	}
	static interface Processor{
		String process(StringStream stream, State state);
	}
	static Set<String> words(String str) {
		Set<String> s = Sets.newHashSet();
		String[] arr = str.split(" ");
		for (String o : arr) {
			s.add(o);
		}
		return s;
	}	
	String ret(String style, String tp,State state) {
	    state.type = tp;
	    return style;
	}
	static final Pattern pattern[] = new Pattern[8];
	static {
	    pattern[0] = Pattern.compile("[\\w\\$_]");
	    pattern[1] = Pattern.compile("[\\w\\\\\\-]");
	    pattern[2] = Pattern.compile("^\\s*\\w*");
	    pattern[3] = Pattern.compile("\\d");
	    pattern[4] = Pattern.compile("[\\w.%]");
	    pattern[5] = Pattern.compile("[,.+>*\\/]");
	    pattern[6] = Pattern.compile("[;{}:\\[\\]]");
	    pattern[7] = Pattern.compile("[\\w\\\\\\-]");
	}
	class tokenBase implements Processor{

		@Override
		public String process(StringStream stream, State state) {
			  stream.eatWhile(pattern[0]);
			    String cur = stream.current();
			    if (keywords.contains(cur)) {
			        return "keyword";
			    }
			    else if (keywords_block.contains(cur)) {
			        return "variable-2";
			    }
			    else if (keywords_important.contains(cur)) {
			        return "string-2";
			    }
			   String ch = stream.next();
			    if (ch.equals("@")) {
			        stream.eatWhile(pattern[1]);
			        return ret("meta", stream.current(),state);
			    }
			    else if (ch.equals("/") && !stream.eat("*").isEmpty()) {
			        state.tokenize = new tokenCComment();
			        return new tokenCComment().process(stream, state);
			    }
			    else if (ch.equals("<") && !stream.eat("!").isEmpty()) {
			        state.tokenize = new tokenSGMLComment();
			        return new tokenSGMLComment().process(stream, state);
			    }
			    else if (ch.equals("=")) ret("", "compare",state);
			    else if ((ch.equals("~") || ch.equals("|")) && !stream.eat("=").isEmpty()) 
			    	return ret("", "compare",state);
			    else if (ch.equals("\"") || ch.equals("'")) {
			        state.tokenize = new tokenString(ch);
			        return state.tokenize.process(stream, state);
			    }
			    else if (ch.equals("#")) {
			        stream.skipToEnd();
			        return ret("comment", "comment",state);
			    }
			    else if (ch.equals("!")) {
			        stream.match(pattern[2]);
			        return ret("keyword", "important",state);
			    }
			    else if (pattern[3].matcher(ch).matches()) {
			        stream.eatWhile(pattern[4]);
			        return ret("number", "unit",state);
			    }
			    else if (pattern[5].matcher(ch).matches()) {
			        return ret("", "select-op",state);
			    }
			    else if (pattern[6].matcher(ch).matches()) {
			        return ret("", ch,state);
			    }
			    else {
			        stream.eatWhile(pattern[7]);
			        return ret("variable", "variable",state);
			    }
			    return "";
		}
		
	}
	class tokenCComment implements Processor{

		@Override
		public String process(StringStream stream, State state) {
			 boolean maybeEnd = false;
			 String ch;
			    while (!(ch = stream.next()).isEmpty()) {
			        if (maybeEnd && ch.equals("/")) {
			            state.tokenize = new tokenBase();
			            break;
			        }
			        maybeEnd = (ch.equals("*"));
			    }
			    return ret("comment", "comment",state);
		}
		
	}
	class tokenSGMLComment implements Processor{

		@Override
		public String process(StringStream stream, State state) {
			 int dashes = 0;
			 String ch;
			    while (!(ch = stream.next()).isEmpty()) {
			        if (dashes >= 2 && ch.equals(">")) {
			            state.tokenize = new tokenBase();
			            break;
			        }
			        dashes = (ch.equals("-")) ? dashes + 1 : 0;
			    }
			    return ret("comment", "comment",state);
		}
		
	}
	class tokenString implements Processor{
		String quote;
		public tokenString(String quote) {
			this.quote = quote;
		}
		@Override
		public String process(StringStream stream, State state) {
			 boolean escaped = false;
			 String ch;
		        while (!(ch = stream.next()).isEmpty()) {
		            if (ch.equals(quote) && !escaped) 
		            	break;
		            escaped = !escaped && ch.equals("\\");
		        }
		        if (!escaped) state.tokenize = new tokenBase();
		        return ret("string", "string",state);
		}
		
	}
	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "nginx");
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-nginx-conf");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("nginx");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(new tokenBase(), new Stack<String>(), "");
	}
	static final Pattern pattern1[] = new Pattern[1];
	static {
	    pattern1[0] = Pattern.compile("^[\\{\\};]$");
	}
	@Override
	public String token(StringStream stream, State state) {
		 if (stream.eatSpace()) 
			 return "";
		    state.type = "";
		    String style = state.tokenize.process(stream, state);
		    String context = state.stack.size()>0?state.stack.get(state.stack.size() -1):"";
		    if (state.type.equals("hash") && context.equals("rule")) 
		    	style = "atom";
		    else if (style.equals("variable")) {
		        if (context.equals("rule"))		        	
		        	style = "number";
		        else if (context.equals("") || context.equals("@media{"))
		        	style = "tag";
		    }
		    if (context.equals("rule") && pattern1[0].matcher(state.type).matches()) {
		    	if(state.stack.size()>0)
		    		state.stack.pop();
		    	}
		    if (state.type.equals("{")) {
		        if (context.equals("@media"))
		        	state.stack.set(state.stack.size() -1, "@media{");
		        else state.stack.push("{");
		    }
		    else if (state.type.equals("}")) {
		    	if(state.stack.size()>0)
		    		state.stack.pop();
		    	}
		    else if (state.type.equals("@media")) 
		    	state.stack.push("@media");
		    else if (context.equals("{") && !state.type.equals("comment"))	    		 
		    	state.stack.push("rule");		    		   
		    return style;
	}

}
