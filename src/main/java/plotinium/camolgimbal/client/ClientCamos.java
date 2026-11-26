package plotinium.camolgimbal.client;

import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import plotinium.camolgimbal.rules.CamoView;

/** The camouflages as the client knows them, from Camol's client copy. An air entry is a removed camo. */
public final class ClientCamos {
    public static final CamoView VIEW = pos -> {
        CamoPosition camo = CamoHelper.CLIENT_CAMO_MAP.get(String.valueOf(pos.asLong()));
        return camo == null || camo.state().isAir() ? null : camo;
    };

    private ClientCamos() {
    }
}
