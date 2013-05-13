create table inputs (
    line_num integer,
    var_id integer,
    pd1 real,
    ps1 real,
    pd2 real,
    ps2 real,
    pd3 real,
    ps3 real,
    tp1 real,
    tp2 real,
    primary key (line_num, var_id)
);

create table blocks (
   var_id integer,
   sensor text,
   iters_to_ignore integer,
   previous_failure integer,
   primary key (var_id, sensor)
);