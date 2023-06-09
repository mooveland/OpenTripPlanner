package org.opentripplanner.api.mapping;

import java.util.List;
import org.opentripplanner.common.model.P2;

public class ElevationMapper {

  public static String mapElevation(List<P2<Double>> pairs) {
    if (pairs == null) {
      return null;
    }
    StringBuilder str = new StringBuilder();
    for (P2<Double> pair : pairs) {
      str.append(Math.round(pair.first));
      str.append(",");
      if (Double.isNaN(pair.second)) {
        str.append("NaN");
      } else {
        str.append(Math.round(pair.second * 10.0) / 10.0);
      }
      str.append(",");
    }
    if (str.length() > 0) {
      str.setLength(str.length() - 1);
    }
    return str.toString();
  }
}
