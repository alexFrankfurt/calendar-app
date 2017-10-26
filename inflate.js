conn = new Mongo();
db = conn.getDB("calendar");
db.persons.insertMany([
  {name: "Matthew A. Carroll", position: "Senior System Architect"},
  {name: "Donald A. Nelson", position: "Software Engineer"},
  {uid: "12189321838919038128888", name: "John E. Burris", position: "Senior Applications Engineer"},
  {name: "Jeff S. Ung", position: "CTO"},
  {name: "Eduardo N. Brown", position: "Systems Software Engineer"},
  {name: "Allan M. McKinney", position: "Software Engineer"},
  {name: "Joseph R. Harvey", position: "Technical Support Specialist"},
  {uid: "1327189379217982137917", name: "Sean L. Hill", position: "Software Engineer"},
  {name: "Gonzalo E. McIlvain", position: "Senior Applications Engineer"},
  {name: "Jeffrey E. Vannorman", position: "Senior Network Engineer"},
  {name: "Brian E. Thompson", position: "Senior Network Architect"},
  {name: "Kristopher B. Hoyle", position: "Web Administrator"},
  {name: "Jesse C. Wise", position: "Software Engineer"},
  {uid: "8193475980217549751753", name: "Ralph I. Stackhouse", position: "Software Engineer"},
  {uid: "0301934321321423423", name: "Joseph B. Fountain", position: "Software Engineer"},
  {name: "Dustin D. Griffith", position: "Software Engineer"},
  {name: "Kevin D. Townsend", position: "Software Engineer"},
  {name: "Gerardo L. Hall", position: "Software Engineer"},
  {name: "Kyle E. Ratliff", position: "Systems Designer"},
  {name: "Mark A. Charles", position: "Security Specialist"},
  {name: "Armando A. Schmidt", position: "Network Engineer"},
  {uid: "234790817438917947", name: "Caleb J. Jacob", position: "Network Engineer"},
  {uid: "3247984728974392", name: "Gary G. Conley", position: "Network Engineer"}
]);

db.vacations.insertMany([
  {uid: "234790817438917947", startDate: ISODate("2017-10-15"), endDate: ISODate("2017-10-22")},
  {uid: "234790817438917947", startDate: ISODate("2017-11-03"), endDate: ISODate("2017-11-17")},
  {uid: "0301934321321423423", startDate: ISODate("2015-04-04"), endDate: ISODate("2015-04-24")},
  {uid: "0301934321321423423", startDate: ISODate("2016-04-01"), endDate: ISODate("2016-04-25")},
  {uid: "12189321838919038128888", startDate: ISODate("2014-06-29"), endDate: ISODate("2014-07-10")},
  {uid: "12189321838919038128888", startDate: ISODate("2014-09-04"), endDate: ISODate("2014-09-08")}
]);
