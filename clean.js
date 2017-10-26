conn = new Mongo();
db = conn.getDB("calendar");
print("hello");

db.persons.remove({});
db.vacations.remove({});

// db.persons.aggregate([ 
//     { $lookup: { from: "vacations", 
//                  localField: "uid", 
//                  foreignField: "uid", 
//                  as: "res_uid" }},
//     {$match: { "res_uid": {$ne: []}}},
//     {$elemMatch: {}}
// ]);
