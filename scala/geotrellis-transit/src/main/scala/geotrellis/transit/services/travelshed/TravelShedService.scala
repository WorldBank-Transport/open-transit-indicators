package geotrellis.transit.services.travelshed

import javax.ws.rs._

@Path("/travelshed")
class TravelShedService extends VectorResource 
                           with WmsResource
                           with ExportResource
