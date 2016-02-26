package es.tid.pce.management;

/**
 * Information of a PCEP Peer
 * described in draft-pkd-pce-pcep-yang-02 
 *  +--ro peers
              +--ro peer* [addr]
                 +--ro addr                        inet:ip-address
                 +--ro role?                       pcep-role
                 +--ro pce-info
                 |  +--ro scope
                 |  |  +--ro intra-area-scope?           boolean
                 |  |  +--ro intra-area-pref?            uint8
                 |  |  +--ro inter-area-scope?           boolean
                 |  |  +--ro inter-area-scope-default?   boolean
                 |  |  +--ro inter-area-pref?            uint8
                 |  |  +--ro inter-as-scope?             boolean
                 |  |  +--ro inter-as-scope-default?     boolean
                 |  |  +--ro inter-as-pref?              uint8
                 |  |  +--ro inter-layer-scope?          boolean
                 |  |  +--ro inter-layer-pref?           uint8
                 |  +--ro domain
                 |  |  +--ro domain-type?   pce-domain-type
                 |  |  +--ro domain?        pce-domain
                 |  +--ro neigh-domains
                 |  |  +--ro domain* [domain-type domain]
                 |  |     +--ro domain-type    pce-domain-type
                 |  |     +--ro domain         pce-domain
                 |  +--ro capability
                 |     +--ro gmpls?                 boolean
                 |     +--ro bi-dir?                boolean
                 |     +--ro diverse?               boolean
                 |     +--ro load-balance?          boolean
                 |     +--ro synchronize?           boolean
                 |     +--ro objective-function?    boolean
                 |     +--ro add-path-constraint?   boolean
                 |     +--ro prioritization?        boolean
                 |     +--ro multi-request?         boolean
                 |     +--ro gco?                   boolean
                 |     +--ro p2mp?                  boolean
                 +--ro discontinuity-time?         yang:timestamp
                 +--ro initiate-session?           boolean
                 +--ro session-exists?             boolean
                 +--ro num-sess-setup-ok?          yang:counter32
                 +--ro num-sess-setup-fail?        yang:counter32
                 +--ro session-up-time?            yang:timestamp
                 +--ro session-fail-time?          yang:timestamp
                 +--ro session-fail-up-time?       yang:timestamp
                 +--ro avg-rsp-time?               uint32
                 +--ro lwm-rsp-time?               uint32
                 +--ro hwm-rsp-time?               uint32
                 +--ro num-pcreq-sent?             yang:counter32
                 +--ro num-pcreq-rcvd?             yang:counter32
                 +--ro num-pcrep-sent?             yang:counter32
                 +--ro num-pcrep-rcvd?             yang:counter32
                 +--ro num-pcerr-sent?             yang:counter32
                 +--ro num-pcerr-rcvd?             yang:counter32
                 +--ro num-pcntf-sent?             yang:counter32
                 +--ro num-pcntf-rcvd?             yang:counter32
                 +--ro num-keepalive-sent?         yang:counter32
                 +--ro num-keepalive-rcvd?         yang:counter32
                 +--ro num-unknown-rcvd?           yang:counter32
                 +--ro num-corrupt-rcvd?           yang:counter32
                 +--ro num-req-sent?               yang:counter32
                 +--ro num-svec-sent?              yang:counter32
                 +--ro num-svec-req-sent?          yang:counter32
                 +--ro num-req-sent-pend-rep?      yang:counter32
                 +--ro num-req-sent-ero-rcvd?      yang:counter32
                 +--ro num-req-sent-nopath-rcvd?   yang:counter32
                 +--ro num-req-sent-cancel-rcvd?   yang:counter32
                 +--ro num-req-sent-error-rcvd?    yang:counter32
                 +--ro num-req-sent-timeout?       yang:counter32



Pobbathi, et al.         Expires August 27, 2015                [Page 9]

 
Internet-Draft                  PCE-YANG                   February 2015


                 +--ro num-req-sent-cancel-sent?   yang:counter32
                 +--ro num-req-rcvd?               yang:counter32
                 +--ro num-svec-rcvd?              yang:counter32
                 +--ro num-svec-req-rcvd?          yang:counter32
                 +--ro num-req-rcvd-pend-rep?      yang:counter32
                 +--ro num-req-rcvd-ero-sent?      yang:counter32
                 +--ro num-req-rcvd-nopath-sent?   yang:counter32
                 +--ro num-req-rcvd-cancel-sent?   yang:counter32
                 +--ro num-req-rcvd-error-sent?    yang:counter32
                 +--ro num-req-rcvd-cancel-rcvd?   yang:counter32
                 +--ro num-rep-rcvd-unknown?       yang:counter32
                 +--ro num-req-rcvd-unknown?       yang:counter32
                 +--ro num-req-sent-closed?        yang:counter32
                 +--ro num-req-rcvd-closed?        yang:counter32
                 +--ro sessions
                    +--ro session* [initiator]
                       +--ro initiator                   pcep-initiator
                       +--ro state-last-change?          yang:timestamp
                       +--ro state?                      pcep-sess-state
                       +--ro connect-retry?              yang:counter32
                       +--ro local-id?                   uint32
                       +--ro remote-id?                  uint32
                       +--ro keepalive-timer?            uint32
                       +--ro peer-keepalive-timer?       uint32
                       +--ro dead-timer?                 uint32
                       +--ro peer-dead-timer?            uint32
                       +--ro ka-hold-time-rem?           uint32
                       +--ro overloaded?                 boolean
                       +--ro overload-time?              uint32
                       +--ro peer-overloaded?            boolean
                       +--ro peer-overload-time?         uint32
                       +--ro discontinuity-time?         yang:timestamp
                       +--ro avg-rsp-time?               uint32
                       +--ro lwm-rsp-time?               uint32
                       +--ro hwm-rsp-time?               uint32
                       +--ro num-pcreq-sent?             yang:counter32
                       +--ro num-pcreq-rcvd?             yang:counter32
                       +--ro num-pcrep-sent?             yang:counter32
                       +--ro num-pcrep-rcvd?             yang:counter32
                       +--ro num-pcerr-sent?             yang:counter32
                       +--ro num-pcerr-rcvd?             yang:counter32
                       +--ro num-pcntf-sent?             yang:counter32
                       +--ro num-pcntf-rcvd?             yang:counter32
                       +--ro num-keepalive-sent?         yang:counter32
                       +--ro num-keepalive-rcvd?         yang:counter32
                       +--ro num-unknown-rcvd?           yang:counter32
                       +--ro num-corrupt-rcvd?           yang:counter32
                       +--ro num-req-sent?               yang:counter32
                       +--ro num-svec-sent?              yang:counter32
                       +--ro num-svec-req-sent?          yang:counter32
                       +--ro num-req-sent-pend-rep?      yang:counter32
                       +--ro num-req-sent-ero-rcvd?      yang:counter32
                       +--ro num-req-sent-nopath-rcvd?   yang:counter32
                       +--ro num-req-sent-cancel-rcvd?   yang:counter32
                       +--ro num-req-sent-error-rcvd?    yang:counter32
                       +--ro num-req-sent-timeout?       yang:counter32
                       +--ro num-req-sent-cancel-sent?   yang:counter32
                       +--ro num-req-rcvd?               yang:counter32
                       +--ro num-svec-rcvd?              yang:counter32
                       +--ro num-svec-req-rcvd?          yang:counter32
                       +--ro num-req-rcvd-pend-rep?      yang:counter32
                       +--ro num-req-rcvd-ero-sent?      yang:counter32
                       +--ro num-req-rcvd-nopath-sent?   yang:counter32
                       +--ro num-req-rcvd-cancel-sent?   yang:counter32
                       +--ro num-req-rcvd-error-sent?    yang:counter32
                       +--ro num-req-rcvd-cancel-rcvd?   yang:counter32
                       +--ro num-rep-rcvd-unknown?       yang:counter32
                       +--ro num-req-rcvd-unknown?       yang:counter32
 * @author ogondio
 *
 */

public class PcepPeerInformation {
	
	

}
