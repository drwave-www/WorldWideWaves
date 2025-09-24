package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.e_community_africa
import com.worldwidewaves.shared.generated.resources.e_community_asia
import com.worldwidewaves.shared.generated.resources.e_community_europe
import com.worldwidewaves.shared.generated.resources.e_community_middle_east
import com.worldwidewaves.shared.generated.resources.e_community_north_america
import com.worldwidewaves.shared.generated.resources.e_community_oceania
import com.worldwidewaves.shared.generated.resources.e_community_south_america
import com.worldwidewaves.shared.generated.resources.e_country_argentina
import com.worldwidewaves.shared.generated.resources.e_country_australia
import com.worldwidewaves.shared.generated.resources.e_country_brazil
import com.worldwidewaves.shared.generated.resources.e_country_canada
import com.worldwidewaves.shared.generated.resources.e_country_chile
import com.worldwidewaves.shared.generated.resources.e_country_china
import com.worldwidewaves.shared.generated.resources.e_country_colombia
import com.worldwidewaves.shared.generated.resources.e_country_democratic_republic_of_the_congo
import com.worldwidewaves.shared.generated.resources.e_country_egypt
import com.worldwidewaves.shared.generated.resources.e_country_england
import com.worldwidewaves.shared.generated.resources.e_country_france
import com.worldwidewaves.shared.generated.resources.e_country_germany
import com.worldwidewaves.shared.generated.resources.e_country_india
import com.worldwidewaves.shared.generated.resources.e_country_indonesia
import com.worldwidewaves.shared.generated.resources.e_country_iran
import com.worldwidewaves.shared.generated.resources.e_country_italy
import com.worldwidewaves.shared.generated.resources.e_country_japan
import com.worldwidewaves.shared.generated.resources.e_country_kenya
import com.worldwidewaves.shared.generated.resources.e_country_mexico
import com.worldwidewaves.shared.generated.resources.e_country_nigeria
import com.worldwidewaves.shared.generated.resources.e_country_pakistan
import com.worldwidewaves.shared.generated.resources.e_country_peru
import com.worldwidewaves.shared.generated.resources.e_country_philippines
import com.worldwidewaves.shared.generated.resources.e_country_russia
import com.worldwidewaves.shared.generated.resources.e_country_south_africa
import com.worldwidewaves.shared.generated.resources.e_country_south_korea
import com.worldwidewaves.shared.generated.resources.e_country_spain
import com.worldwidewaves.shared.generated.resources.e_country_thailand
import com.worldwidewaves.shared.generated.resources.e_country_turkey
import com.worldwidewaves.shared.generated.resources.e_country_united_arab_emirates
import com.worldwidewaves.shared.generated.resources.e_country_usa
import com.worldwidewaves.shared.generated.resources.e_location_bangalore_india
import com.worldwidewaves.shared.generated.resources.e_location_bangkok_thailand
import com.worldwidewaves.shared.generated.resources.e_location_beijing_china
import com.worldwidewaves.shared.generated.resources.e_location_berlin_germany
import com.worldwidewaves.shared.generated.resources.e_location_bogota_colombia
import com.worldwidewaves.shared.generated.resources.e_location_buenos_aires_argentina
import com.worldwidewaves.shared.generated.resources.e_location_cairo_egypt
import com.worldwidewaves.shared.generated.resources.e_location_chicago_usa
import com.worldwidewaves.shared.generated.resources.e_location_delhi_india
import com.worldwidewaves.shared.generated.resources.e_location_dubai_united_arab_emirates
import com.worldwidewaves.shared.generated.resources.e_location_hong_kong_china
import com.worldwidewaves.shared.generated.resources.e_location_istanbul_turkey
import com.worldwidewaves.shared.generated.resources.e_location_jakarta_indonesia
import com.worldwidewaves.shared.generated.resources.e_location_johannesburg_south_africa
import com.worldwidewaves.shared.generated.resources.e_location_karachi_pakistan
import com.worldwidewaves.shared.generated.resources.e_location_kinshasa_democratic_republic_of_the_congo
import com.worldwidewaves.shared.generated.resources.e_location_lagos_nigeria
import com.worldwidewaves.shared.generated.resources.e_location_lima_peru
import com.worldwidewaves.shared.generated.resources.e_location_london_england
import com.worldwidewaves.shared.generated.resources.e_location_los_angeles_usa
import com.worldwidewaves.shared.generated.resources.e_location_madrid_spain
import com.worldwidewaves.shared.generated.resources.e_location_manila_philippines
import com.worldwidewaves.shared.generated.resources.e_location_melbourne_australia
import com.worldwidewaves.shared.generated.resources.e_location_mexico_city_mexico
import com.worldwidewaves.shared.generated.resources.e_location_moscow_russia
import com.worldwidewaves.shared.generated.resources.e_location_mumbai_india
import com.worldwidewaves.shared.generated.resources.e_location_nairobi_kenya
import com.worldwidewaves.shared.generated.resources.e_location_new_york_usa
import com.worldwidewaves.shared.generated.resources.e_location_paris_france
import com.worldwidewaves.shared.generated.resources.e_location_rome_italy
import com.worldwidewaves.shared.generated.resources.e_location_san_francisco_usa
import com.worldwidewaves.shared.generated.resources.e_location_santiago_chile
import com.worldwidewaves.shared.generated.resources.e_location_sao_paulo_brazil
import com.worldwidewaves.shared.generated.resources.e_location_seoul_south_korea
import com.worldwidewaves.shared.generated.resources.e_location_shanghai_china
import com.worldwidewaves.shared.generated.resources.e_location_sydney_australia
import com.worldwidewaves.shared.generated.resources.e_location_tehran_iran
import com.worldwidewaves.shared.generated.resources.e_location_tokyo_japan
import com.worldwidewaves.shared.generated.resources.e_location_toronto_canada
import com.worldwidewaves.shared.generated.resources.e_location_vancouver_canada
import com.worldwidewaves.shared.generated.resources.e_map_bangalore_india
import com.worldwidewaves.shared.generated.resources.e_map_bangkok_thailand
import com.worldwidewaves.shared.generated.resources.e_map_beijing_china
import com.worldwidewaves.shared.generated.resources.e_map_berlin_germany
import com.worldwidewaves.shared.generated.resources.e_map_bogota_colombia
import com.worldwidewaves.shared.generated.resources.e_map_buenos_aires_argentina
import com.worldwidewaves.shared.generated.resources.e_map_cairo_egypt
import com.worldwidewaves.shared.generated.resources.e_map_chicago_usa
import com.worldwidewaves.shared.generated.resources.e_map_delhi_india
import com.worldwidewaves.shared.generated.resources.e_map_dubai_united_arab_emirates
import com.worldwidewaves.shared.generated.resources.e_map_hong_kong_china
import com.worldwidewaves.shared.generated.resources.e_map_istanbul_turkey
import com.worldwidewaves.shared.generated.resources.e_map_jakarta_indonesia
import com.worldwidewaves.shared.generated.resources.e_map_johannesburg_south_africa
import com.worldwidewaves.shared.generated.resources.e_map_karachi_pakistan
import com.worldwidewaves.shared.generated.resources.e_map_kinshasa_democratic_republic_of_the_congo
import com.worldwidewaves.shared.generated.resources.e_map_lagos_nigeria
import com.worldwidewaves.shared.generated.resources.e_map_lima_peru
import com.worldwidewaves.shared.generated.resources.e_map_london_england
import com.worldwidewaves.shared.generated.resources.e_map_los_angeles_usa
import com.worldwidewaves.shared.generated.resources.e_map_madrid_spain
import com.worldwidewaves.shared.generated.resources.e_map_manila_philippines
import com.worldwidewaves.shared.generated.resources.e_map_melbourne_australia
import com.worldwidewaves.shared.generated.resources.e_map_mexico_city_mexico
import com.worldwidewaves.shared.generated.resources.e_map_moscow_russia
import com.worldwidewaves.shared.generated.resources.e_map_mumbai_india
import com.worldwidewaves.shared.generated.resources.e_map_nairobi_kenya
import com.worldwidewaves.shared.generated.resources.e_map_new_york_usa
import com.worldwidewaves.shared.generated.resources.e_map_paris_france
import com.worldwidewaves.shared.generated.resources.e_map_rome_italy
import com.worldwidewaves.shared.generated.resources.e_map_san_francisco_usa
import com.worldwidewaves.shared.generated.resources.e_map_santiago_chile
import com.worldwidewaves.shared.generated.resources.e_map_sao_paulo_brazil
import com.worldwidewaves.shared.generated.resources.e_map_seoul_south_korea
import com.worldwidewaves.shared.generated.resources.e_map_shanghai_china
import com.worldwidewaves.shared.generated.resources.e_map_sydney_australia
import com.worldwidewaves.shared.generated.resources.e_map_tehran_iran
import com.worldwidewaves.shared.generated.resources.e_map_tokyo_japan
import com.worldwidewaves.shared.generated.resources.e_map_toronto_canada
import com.worldwidewaves.shared.generated.resources.e_map_vancouver_canada
import com.worldwidewaves.shared.generated.resources.not_found
import dev.icerock.moko.resources.StringResource

private val locationImages =
    mapOf(
        "bangalore_india" to Res.drawable.e_location_bangalore_india,
        "bangkok_thailand" to Res.drawable.e_location_bangkok_thailand,
        "beijing_china" to Res.drawable.e_location_beijing_china,
        "berlin_germany" to Res.drawable.e_location_berlin_germany,
        "bogota_colombia" to Res.drawable.e_location_bogota_colombia,
        "buenos_aires_argentina" to Res.drawable.e_location_buenos_aires_argentina,
        "cairo_egypt" to Res.drawable.e_location_cairo_egypt,
        "chicago_usa" to Res.drawable.e_location_chicago_usa,
        "delhi_india" to Res.drawable.e_location_delhi_india,
        "dubai_united_arab_emirates" to Res.drawable.e_location_dubai_united_arab_emirates,
        "hong_kong_china" to Res.drawable.e_location_hong_kong_china,
        "istanbul_turkey" to Res.drawable.e_location_istanbul_turkey,
        "jakarta_indonesia" to Res.drawable.e_location_jakarta_indonesia,
        "johannesburg_south_africa" to Res.drawable.e_location_johannesburg_south_africa,
        "karachi_pakistan" to Res.drawable.e_location_karachi_pakistan,
        "kinshasa_democratic_republic_of_the_congo" to Res.drawable.e_location_kinshasa_democratic_republic_of_the_congo,
        "lagos_nigeria" to Res.drawable.e_location_lagos_nigeria,
        "lima_peru" to Res.drawable.e_location_lima_peru,
        "london_england" to Res.drawable.e_location_london_england,
        "los_angeles_usa" to Res.drawable.e_location_los_angeles_usa,
        "madrid_spain" to Res.drawable.e_location_madrid_spain,
        "manila_philippines" to Res.drawable.e_location_manila_philippines,
        "melbourne_australia" to Res.drawable.e_location_melbourne_australia,
        "mexico_city_mexico" to Res.drawable.e_location_mexico_city_mexico,
        "moscow_russia" to Res.drawable.e_location_moscow_russia,
        "mumbai_india" to Res.drawable.e_location_mumbai_india,
        "nairobi_kenya" to Res.drawable.e_location_nairobi_kenya,
        "new_york_usa" to Res.drawable.e_location_new_york_usa,
        "paris_france" to Res.drawable.e_location_paris_france,
        "rome_italy" to Res.drawable.e_location_rome_italy,
        "san_francisco_usa" to Res.drawable.e_location_san_francisco_usa,
        "santiago_chile" to Res.drawable.e_location_santiago_chile,
        "sao_paulo_brazil" to Res.drawable.e_location_sao_paulo_brazil,
        "seoul_south_korea" to Res.drawable.e_location_seoul_south_korea,
        "shanghai_china" to Res.drawable.e_location_shanghai_china,
        "sydney_australia" to Res.drawable.e_location_sydney_australia,
        "tehran_iran" to Res.drawable.e_location_tehran_iran,
        "tokyo_japan" to Res.drawable.e_location_tokyo_japan,
        "toronto_canada" to Res.drawable.e_location_toronto_canada,
        "vancouver_canada" to Res.drawable.e_location_vancouver_canada,
    )

private val mapImages =
    mapOf(
        "bangalore_india" to Res.drawable.e_map_bangalore_india,
        "bangkok_thailand" to Res.drawable.e_map_bangkok_thailand,
        "beijing_china" to Res.drawable.e_map_beijing_china,
        "berlin_germany" to Res.drawable.e_map_berlin_germany,
        "bogota_colombia" to Res.drawable.e_map_bogota_colombia,
        "buenos_aires_argentina" to Res.drawable.e_map_buenos_aires_argentina,
        "cairo_egypt" to Res.drawable.e_map_cairo_egypt,
        "chicago_usa" to Res.drawable.e_map_chicago_usa,
        "delhi_india" to Res.drawable.e_map_delhi_india,
        "dubai_united_arab_emirates" to Res.drawable.e_map_dubai_united_arab_emirates,
        "hong_kong_china" to Res.drawable.e_map_hong_kong_china,
        "istanbul_turkey" to Res.drawable.e_map_istanbul_turkey,
        "jakarta_indonesia" to Res.drawable.e_map_jakarta_indonesia,
        "johannesburg_south_africa" to Res.drawable.e_map_johannesburg_south_africa,
        "karachi_pakistan" to Res.drawable.e_map_karachi_pakistan,
        "kinshasa_democratic_republic_of_the_congo" to Res.drawable.e_map_kinshasa_democratic_republic_of_the_congo,
        "lagos_nigeria" to Res.drawable.e_map_lagos_nigeria,
        "lima_peru" to Res.drawable.e_map_lima_peru,
        "london_england" to Res.drawable.e_map_london_england,
        "los_angeles_usa" to Res.drawable.e_map_los_angeles_usa,
        "madrid_spain" to Res.drawable.e_map_madrid_spain,
        "manila_philippines" to Res.drawable.e_map_manila_philippines,
        "melbourne_australia" to Res.drawable.e_map_melbourne_australia,
        "mexico_city_mexico" to Res.drawable.e_map_mexico_city_mexico,
        "moscow_russia" to Res.drawable.e_map_moscow_russia,
        "mumbai_india" to Res.drawable.e_map_mumbai_india,
        "nairobi_kenya" to Res.drawable.e_map_nairobi_kenya,
        "new_york_usa" to Res.drawable.e_map_new_york_usa,
        "paris_france" to Res.drawable.e_map_paris_france,
        "rome_italy" to Res.drawable.e_map_rome_italy,
        "san_francisco_usa" to Res.drawable.e_map_san_francisco_usa,
        "santiago_chile" to Res.drawable.e_map_santiago_chile,
        "sao_paulo_brazil" to Res.drawable.e_map_sao_paulo_brazil,
        "seoul_south_korea" to Res.drawable.e_map_seoul_south_korea,
        "shanghai_china" to Res.drawable.e_map_shanghai_china,
        "sydney_australia" to Res.drawable.e_map_sydney_australia,
        "tehran_iran" to Res.drawable.e_map_tehran_iran,
        "tokyo_japan" to Res.drawable.e_map_tokyo_japan,
        "toronto_canada" to Res.drawable.e_map_toronto_canada,
        "vancouver_canada" to Res.drawable.e_map_vancouver_canada,
    )

private val communityImages =
    mapOf(
        "africa" to Res.drawable.e_community_africa,
        "asia" to Res.drawable.e_community_asia,
        "europe" to Res.drawable.e_community_europe,
        "middle_east" to Res.drawable.e_community_middle_east,
        "north_america" to Res.drawable.e_community_north_america,
        "oceania" to Res.drawable.e_community_oceania,
        "south_america" to Res.drawable.e_community_south_america,
    )

private val countryImages =
    mapOf(
        "argentina" to Res.drawable.e_country_argentina,
        "australia" to Res.drawable.e_country_australia,
        "brazil" to Res.drawable.e_country_brazil,
        "canada" to Res.drawable.e_country_canada,
        "chile" to Res.drawable.e_country_chile,
        "china" to Res.drawable.e_country_china,
        "colombia" to Res.drawable.e_country_colombia,
        "democratic_republic_of_the_congo" to Res.drawable.e_country_democratic_republic_of_the_congo,
        "egypt" to Res.drawable.e_country_egypt,
        "england" to Res.drawable.e_country_england,
        "france" to Res.drawable.e_country_france,
        "germany" to Res.drawable.e_country_germany,
        "india" to Res.drawable.e_country_india,
        "indonesia" to Res.drawable.e_country_indonesia,
        "iran" to Res.drawable.e_country_iran,
        "italy" to Res.drawable.e_country_italy,
        "japan" to Res.drawable.e_country_japan,
        "kenya" to Res.drawable.e_country_kenya,
        "mexico" to Res.drawable.e_country_mexico,
        "nigeria" to Res.drawable.e_country_nigeria,
        "pakistan" to Res.drawable.e_country_pakistan,
        "peru" to Res.drawable.e_country_peru,
        "philippines" to Res.drawable.e_country_philippines,
        "russia" to Res.drawable.e_country_russia,
        "south_africa" to Res.drawable.e_country_south_africa,
        "south_korea" to Res.drawable.e_country_south_korea,
        "spain" to Res.drawable.e_country_spain,
        "thailand" to Res.drawable.e_country_thailand,
        "turkey" to Res.drawable.e_country_turkey,
        "united_arab_emirates" to Res.drawable.e_country_united_arab_emirates,
        "usa" to Res.drawable.e_country_usa,
    )

fun getEventImage(
    type: String,
    id: String,
): Any? =
    when (type) {
        "location" -> locationImages[id] ?: Res.drawable.not_found
        "map" -> mapImages[id] ?: Res.drawable.not_found
        "community" -> communityImages[id] ?: Res.drawable.not_found
        "country" -> countryImages[id] ?: Res.drawable.not_found
        else -> Res.drawable.not_found
    }

private val countryTexts =
    mapOf(
        "usa" to MokoRes.strings.country_usa,
        "mexico" to MokoRes.strings.country_mexico,
        "brazil" to MokoRes.strings.country_brazil,
        "argentina" to MokoRes.strings.country_argentina,
        "england" to MokoRes.strings.country_england,
        "france" to MokoRes.strings.country_france,
        "germany" to MokoRes.strings.country_germany,
        "spain" to MokoRes.strings.country_spain,
        "italy" to MokoRes.strings.country_italy,
        "russia" to MokoRes.strings.country_russia,
        "turkey" to MokoRes.strings.country_turkey,
        "egypt" to MokoRes.strings.country_egypt,
        "south_africa" to MokoRes.strings.country_south_africa,
        "kenya" to MokoRes.strings.country_kenya,
        "nigeria" to MokoRes.strings.country_nigeria,
        "democratic_republic_of_the_congo" to MokoRes.strings.country_democratic_republic_of_the_congo,
        "united_arab_emirates" to MokoRes.strings.country_united_arab_emirates,
        "india" to MokoRes.strings.country_india,
        "indonesia" to MokoRes.strings.country_indonesia,
        "thailand" to MokoRes.strings.country_thailand,
        "philippines" to MokoRes.strings.country_philippines,
        "japan" to MokoRes.strings.country_japan,
        "south_korea" to MokoRes.strings.country_south_korea,
        "china" to MokoRes.strings.country_china,
        "australia" to MokoRes.strings.country_australia,
        "canada" to MokoRes.strings.country_canada,
        "peru" to MokoRes.strings.country_peru,
        "colombia" to MokoRes.strings.country_colombia,
        "chile" to MokoRes.strings.country_chile,
        "iran" to MokoRes.strings.country_iran,
        "pakistan" to MokoRes.strings.country_pakistan,
    )

private val communityTexts =
    mapOf(
        "north_america" to MokoRes.strings.community_north_america,
        "south_america" to MokoRes.strings.community_south_america,
        "europe" to MokoRes.strings.community_europe,
        "africa" to MokoRes.strings.community_africa,
        "middle_east" to MokoRes.strings.community_middle_east,
        "asia" to MokoRes.strings.community_asia,
        "oceania" to MokoRes.strings.community_oceania,
    )

fun getCountryText(id: String?): StringResource = countryTexts[id] ?: MokoRes.strings.empty

fun getCommunityText(id: String?): StringResource = communityTexts[id] ?: MokoRes.strings.empty

private val eventLocationTexts =
    mapOf(
        "new_york_usa" to MokoRes.strings.event_location_new_york_usa,
        "los_angeles_usa" to MokoRes.strings.event_location_los_angeles_usa,
        "mexico_city_mexico" to MokoRes.strings.event_location_mexico_city_mexico,
        "sao_paulo_brazil" to MokoRes.strings.event_location_sao_paulo_brazil,
        "buenos_aires_argentina" to MokoRes.strings.event_location_buenos_aires_argentina,
        "london_england" to MokoRes.strings.event_location_london_england,
        "paris_france" to MokoRes.strings.event_location_paris_france,
        "berlin_germany" to MokoRes.strings.event_location_berlin_germany,
        "madrid_spain" to MokoRes.strings.event_location_madrid_spain,
        "rome_italy" to MokoRes.strings.event_location_rome_italy,
        "moscow_russia" to MokoRes.strings.event_location_moscow_russia,
        "istanbul_turkey" to MokoRes.strings.event_location_istanbul_turkey,
        "cairo_egypt" to MokoRes.strings.event_location_cairo_egypt,
        "johannesburg_south_africa" to MokoRes.strings.event_location_johannesburg_south_africa,
        "nairobi_kenya" to MokoRes.strings.event_location_nairobi_kenya,
        "lagos_nigeria" to MokoRes.strings.event_location_lagos_nigeria,
        "kinshasa_democratic_republic_of_the_congo" to MokoRes.strings.event_location_kinshasa_democratic_republic_of_the_congo,
        "dubai_united_arab_emirates" to MokoRes.strings.event_location_dubai_united_arab_emirates,
        "mumbai_india" to MokoRes.strings.event_location_mumbai_india,
        "delhi_india" to MokoRes.strings.event_location_delhi_india,
        "bangalore_india" to MokoRes.strings.event_location_bangalore_india,
        "jakarta_indonesia" to MokoRes.strings.event_location_jakarta_indonesia,
        "bangkok_thailand" to MokoRes.strings.event_location_bangkok_thailand,
        "manila_philippines" to MokoRes.strings.event_location_manila_philippines,
        "tokyo_japan" to MokoRes.strings.event_location_tokyo_japan,
        "seoul_south_korea" to MokoRes.strings.event_location_seoul_south_korea,
        "beijing_china" to MokoRes.strings.event_location_beijing_china,
        "shanghai_china" to MokoRes.strings.event_location_shanghai_china,
        "hong_kong_china" to MokoRes.strings.event_location_hong_kong_china,
        "sydney_australia" to MokoRes.strings.event_location_sydney_australia,
        "melbourne_australia" to MokoRes.strings.event_location_melbourne_australia,
        "toronto_canada" to MokoRes.strings.event_location_toronto_canada,
        "vancouver_canada" to MokoRes.strings.event_location_vancouver_canada,
        "chicago_usa" to MokoRes.strings.event_location_chicago_usa,
        "san_francisco_usa" to MokoRes.strings.event_location_san_francisco_usa,
        "lima_peru" to MokoRes.strings.event_location_lima_peru,
        "bogota_colombia" to MokoRes.strings.event_location_bogota_colombia,
        "santiago_chile" to MokoRes.strings.event_location_santiago_chile,
        "tehran_iran" to MokoRes.strings.event_location_tehran_iran,
        "karachi_pakistan" to MokoRes.strings.event_location_karachi_pakistan,
    )

private val eventDescriptionTexts =
    mapOf(
        "new_york_usa" to MokoRes.strings.event_description_new_york_usa,
        "los_angeles_usa" to MokoRes.strings.event_description_los_angeles_usa,
        "mexico_city_mexico" to MokoRes.strings.event_description_mexico_city_mexico,
        "sao_paulo_brazil" to MokoRes.strings.event_description_sao_paulo_brazil,
        "buenos_aires_argentina" to MokoRes.strings.event_description_buenos_aires_argentina,
        "london_england" to MokoRes.strings.event_description_london_england,
        "paris_france" to MokoRes.strings.event_description_paris_france,
        "berlin_germany" to MokoRes.strings.event_description_berlin_germany,
        "madrid_spain" to MokoRes.strings.event_description_madrid_spain,
        "rome_italy" to MokoRes.strings.event_description_rome_italy,
        "moscow_russia" to MokoRes.strings.event_description_moscow_russia,
        "istanbul_turkey" to MokoRes.strings.event_description_istanbul_turkey,
        "cairo_egypt" to MokoRes.strings.event_description_cairo_egypt,
        "johannesburg_south_africa" to MokoRes.strings.event_description_johannesburg_south_africa,
        "nairobi_kenya" to MokoRes.strings.event_description_nairobi_kenya,
        "lagos_nigeria" to MokoRes.strings.event_description_lagos_nigeria,
        "kinshasa_democratic_republic_of_the_congo" to MokoRes.strings.event_description_kinshasa_democratic_republic_of_the_congo,
        "dubai_united_arab_emirates" to MokoRes.strings.event_description_dubai_united_arab_emirates,
        "mumbai_india" to MokoRes.strings.event_description_mumbai_india,
        "delhi_india" to MokoRes.strings.event_description_delhi_india,
        "bangalore_india" to MokoRes.strings.event_description_bangalore_india,
        "jakarta_indonesia" to MokoRes.strings.event_description_jakarta_indonesia,
        "bangkok_thailand" to MokoRes.strings.event_description_bangkok_thailand,
        "manila_philippines" to MokoRes.strings.event_description_manila_philippines,
        "tokyo_japan" to MokoRes.strings.event_description_tokyo_japan,
        "seoul_south_korea" to MokoRes.strings.event_description_seoul_south_korea,
        "beijing_china" to MokoRes.strings.event_description_beijing_china,
        "shanghai_china" to MokoRes.strings.event_description_shanghai_china,
        "hong_kong_china" to MokoRes.strings.event_description_hong_kong_china,
        "sydney_australia" to MokoRes.strings.event_description_sydney_australia,
        "melbourne_australia" to MokoRes.strings.event_description_melbourne_australia,
        "toronto_canada" to MokoRes.strings.event_description_toronto_canada,
        "vancouver_canada" to MokoRes.strings.event_description_vancouver_canada,
        "chicago_usa" to MokoRes.strings.event_description_chicago_usa,
        "san_francisco_usa" to MokoRes.strings.event_description_san_francisco_usa,
        "lima_peru" to MokoRes.strings.event_description_lima_peru,
        "bogota_colombia" to MokoRes.strings.event_description_bogota_colombia,
        "santiago_chile" to MokoRes.strings.event_description_santiago_chile,
        "tehran_iran" to MokoRes.strings.event_description_tehran_iran,
        "karachi_pakistan" to MokoRes.strings.event_description_karachi_pakistan,
    )

fun getEventText(
    type: String,
    id: String,
): StringResource =
    when (type) {
        "location" -> eventLocationTexts[id] ?: MokoRes.strings.empty
        "description" -> eventDescriptionTexts[id] ?: MokoRes.strings.empty
        else -> throw IllegalArgumentException("Unknown text type $type for event")
    }
