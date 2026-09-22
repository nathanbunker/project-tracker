package org.dandeliondaily.weeklyreport.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Session;
import org.openimmunizationsoftware.pt.doa.WorkObligationDao;
import org.openimmunizationsoftware.pt.model.WorkObligation;

public class WorkObligationService {
    public static final int DEFAULT_OBLIGATED_MINUTES = 2250;

    public Map<LocalDate, WorkObligation> loadRaw(Session session, int ownerUserId, LocalDate startInclusive,
            LocalDate endExclusive) {
        Map<LocalDate, WorkObligation> result = new LinkedHashMap<LocalDate, WorkObligation>();
        for (WorkObligation obligation : new WorkObligationDao(session).listForRange(ownerUserId, startInclusive,
                endExclusive)) {
            result.put(obligation.getWeekStartLocalDate(), obligation);
        }
        return result;
    }

    public int resolveMinutes(Map<LocalDate, WorkObligation> raw, LocalDate weekStart) {
        WorkObligation obligation = raw.get(weekStart);
        return obligation == null ? DEFAULT_OBLIGATED_MINUTES : obligation.getObligatedMinutes();
    }
}
