package com.met.trading.rules;

import java.util.ArrayList;
import java.util.List;

public class RuleResult {

    private boolean ok = true;
    private boolean blocking;
    private final List<String> warnings = new ArrayList<String>();

    public boolean isOk() {
        return ok;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public RuleResult error(String value) {
        ok = false;
        blocking = true;
        warnings.add(value);
        return this;
    }

    public RuleResult warning(String value) {
        warnings.add(value);
        return this;
    }

    public RuleResult merge(RuleResult other) {
        if (other == null) {
            return this;
        }
        if (!other.ok) {
            ok = false;
        }
        if (other.blocking) {
            blocking = true;
        }
        warnings.addAll(other.warnings);
        return this;
    }
}
