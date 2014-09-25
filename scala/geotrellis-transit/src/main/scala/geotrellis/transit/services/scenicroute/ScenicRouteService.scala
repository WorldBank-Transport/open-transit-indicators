package geotrellis.transit.services.scenicroute

import javax.ws.rs._

@Path("/scenicroute")
class ScenicRouteService extends WmsResource
                            with ExportResource
                            with VectorResource
