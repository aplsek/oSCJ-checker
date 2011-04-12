/**
 *  Name: Railsegment
 *  Author : Kelvin Nilsen, <kelvin.nilsen@atego.com>
 *
 *  Copyright (C) 2011  Kelvin Nilsen
 *
 *  Railsegment is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  Railsegment is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with Railsegment; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package railsegment;

import static javax.safetycritical.annotate.Level.LEVEL_2;

import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.Scope;


// This class represents the navigation info (lat/long of each rail
// segment, speed limit along each rail segment, track switch
// locations and desired track switch settings, and the time schedule
// for particular rendezvous points along the way.)


@Scope("D")
@SCJAllowed(value=LEVEL_2, members=true)
public class RouteData {

  // no details provided.  i'm assuming this detail is not necessary.

}

