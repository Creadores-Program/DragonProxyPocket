package io.netty.handler.ipfilter;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class RuleBasedIpFilter extends AbstractRemoteAddressFilter<InetSocketAddress> {
   private final IpFilterRule[] rules;

   public RuleBasedIpFilter(IpFilterRule... rules) {
      if (rules == null) {
         throw new NullPointerException("rules");
      } else {
         this.rules = rules;
      }
   }

   protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception {
      IpFilterRule[] arr$ = this.rules;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         IpFilterRule rule = arr$[i$];
         if (rule == null) {
            break;
         }

         if (rule.matches(remoteAddress)) {
            return rule.ruleType() == IpFilterRuleType.ACCEPT;
         }
      }

      return true;
   }
}
