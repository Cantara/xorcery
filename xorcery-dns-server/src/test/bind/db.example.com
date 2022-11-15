$TTL	3600
@	IN	SOA	example.com. root.example.com. (
			       1		; Serial
			    3600		; Refresh
			     600		; Retry
			   86400		; Expire
			     600 )	    ; Negative Cache TTL
;
@	IN	NS	ns1.example.com.
@	IN	A	127.0.0.1
@	IN	AAAA	::1

ns1			A	192.168.1.107		; Change to desired NS1 IP
xorcery1			A	192.168.1.107
xorcery2			A	192.168.1.107

analytics			A	192.168.1.107
analytics			A	127.0.0.1

_analytics._tcp.example.com. SRV 10 30 8080 xorcery1.example.com.
_analytics._tcp.example.com. SRV 10 70 8888 xorcery2.example.com.
_analytics._tcp.example.com. TXT "api_ver=v1.0,v1.1,v1.2,v1.3" "api_scheme=http" "api_path=/api/foo" "pri=0" "api_auth=false"
