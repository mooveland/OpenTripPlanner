/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.Hungarian = {

    config :
    {
        //Name of a language written in a language itself (Used in Frontend to
        //choose a language)
        name: 'Magyar',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true,
        //Name of localization file (*.po file) in src/client/i18n
        locale_short : "hu",
        //Name of datepicker localization in
        //src/client/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into src/client/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "hu"

    },

    /**
     * Info Widgets: a list of the non-module-specific "information widgets"
     * that can be accessed from the top bar of the client display. Expressed as
     * an array of objects, where each object has the following fields:
     * - content: <string> the HTML content of the widget
     * - [title]: <string> the title of the widget
     * - [cssClass]: <string> the name of a CSS class to apply to the widget.
     * If not specified, the default styling is used.
     */
    infoWidgets : [
            {
                title: 'Névjegy',
                content: '<p>Az oldal névjegye</p>',
                //cssClass: 'otp-contactWidget',
            },
            {
                title: 'Kapcsolat',
                content: '<p>Észrevételei vannak? Lépjen velünk kapcsolatba…</p>'
            },
    ],


    time:
    {
        format         : "YYYY. MMMM D. HH:mm", //moment.js
        date_format    : "YYYY.MM.DD.", //momentjs must be same as date_picker format which is by default: mm/dd/yy
        time_format    : "HH:mm", //momentjs
        time_format_picker : "HH:mm", //http://trentrichardson.com/examples/timepicker/#tp-formatting
    },

    CLASS_NAME : "otp.locale.English"
};