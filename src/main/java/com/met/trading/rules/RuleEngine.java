package com.met.trading.rules;

import com.meridianenergy.etrm.deal.Deal;
import com.met.trading.rules.common.RefLookup;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class RuleEngine {

    private static final Logger LOG =
            Logger.getLogger(RuleEngine.class.getName());

    public RuleResult run(String phase, Deal deal, RefLookup lookup) {
        RuleResult result = new RuleResult();
        for (TradingRule rule : load(phase)) {
            result.merge(rule.execute(new RuleContext(deal, lookup)));
        }
        return result;
    }

    public List<TradingRule> load(String phase) {
        List<TradingRule> loaded = new ArrayList<TradingRule>();
        Properties properties = new Properties();
        try {
            InputStream input =
                    RuleEngine.class.getResourceAsStream("/rules.properties");
            if (input != null) {
                try {
                    properties.load(input);
                } finally {
                    input.close();
                }
            }
            String configured = properties.getProperty(phase + ".rules", "");
            String[] names = configured.split(",");
            for (String name : names) {
                loadOne(name, loaded);
            }
        } catch (Exception e) {
            LOG.warning("Unable to load rules: " + e.getMessage());
        }
        return loaded;
    }

    private void loadOne(String name, List<TradingRule> loaded) {
        if (name == null || name.trim().length() == 0) {
            return;
        }
        String className = name.trim();
        try {
            Class<?> type = Class.forName(className);
            Object instance = type.newInstance();
            loaded.add((TradingRule) instance);
        } catch (ClassNotFoundException e) {
            // swallow - desks add rules to the properties file before the class ships
            LOG.warning("Rule class not found: " + className);
        } catch (Exception e) {
            LOG.warning("Rule load failed: " + className + " "
                    + e.getMessage());
        }
    }
}
