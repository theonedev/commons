Changex[GeList_List, Pc_Integer, {f_Function, Kind_String}] := Block[
  {GeCopy = GeList},
  Switch[(*考虑多种变化选项*)
   Kind,
   "Change", GeCopy[[Pc]] = f[GeCopy[[Pc]]],
   "Add", GeCopy[[Pc]] += f[GeCopy[[Pc]]]]; GeCopy
  ]

GeChange[p_Real, GeList_List, n_Integer, {f_Function, Kind_String}] :=
 (*突变概率，基因序列，突变n次，｛突变函数，突变模式｝*)
 Block[
  {Len = Length[GeList],
   Pc = 0,
   GeCopy = 0
   },
  If[RandomReal[] < p,
   GeCopy = GeList;
   Nest[Changex[#, RandomInteger[{1, Len}], {f, Kind}] &, GeCopy, n](*n次迭代*)
   , GeList]
  ]

GeChangeList[GeList_List, pList_List, {f_Function, Kind_String}] :=
 (*基因序列，突变序列，｛突变函数，突变模式｝*)
 Block[
  {Len = Length[GeList],
   Pc = 0,
   GeCopy = GeList
   },
  Do[GeCopy = Changex[GeCopy, pList[[i]], {f, Kind}];, {i, 1, Length[pList]}];
  GeCopy
  ]

GePMC[GeList_List, {S_Integer, T_Integer}] :=(*部分重排序*)
 (*需要突变的基因，｛开始位，结束位｝*)
 Module[{GeCopy = GeList,
   head = GeList[[1 ;; S - 1]],
   body = GeList[[S ;; T]],
   foot = GeList[[T + 1 ;; Length[GeList]]]
   },
  head~Join~Reverse[body]~Join~foot
  ]