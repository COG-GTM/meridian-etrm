package com.met.trading.rules;

public interface TradingRule {

    String getRuleName();

    RuleResult execute(RuleContext context);
}
