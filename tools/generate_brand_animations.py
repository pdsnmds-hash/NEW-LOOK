import json, pathlib
out=pathlib.Path('app/src/main/assets/animations')
purple=[.745,.702,1,1]; mint=[.45,.84,.75,1]
def prop(v): return {'a':0,'k':v}
def anim(vals):
 return {'a':1,'k':[{'t':t,'s':v,'e':vals[i+1][1],'o':{'x':.33,'y':0},'i':{'x':.67,'y':1}} if i<len(vals)-1 else {'t':t,'s':v} for i,(t,v) in enumerate(vals)]}
def stroke(c,w=6): return {'ty':'st','c':prop(c),'o':prop(100),'w':prop(w),'lc':2,'lj':2}
def path(points): return {'ty':'sh','ks':prop({'i':[[0,0]]*len(points),'o':[[0,0]]*len(points),'v':points,'c':False})}
def layer(name,shapes,pos=None,scale=None):
 return {'ty':4,'nm':name,'ind':len(layers)+1,'ip':0,'op':90,'st':0,'ks':{'o':prop(100),'r':prop(0),'p':pos or prop([120,120,0]),'a':prop([0,0,0]),'s':scale or prop([100,100,100])},'shapes':shapes}
def save(name):
 (out/name).write_text(json.dumps({'v':'5.12.2','fr':60,'ip':0,'op':90,'w':240,'h':240,'nm':name,'ddd':0,'assets':[],'layers':layers},separators=(',',':')))
layers=[]
layers.append(layer('Check', [path([[-26,0],[-7,20],[29,-22]]),stroke(mint,9),{'ty':'tm','s':prop(0),'e':anim([(0,[0]),(15,[0]),(43,[100]),(90,[100])]),'o':prop(0),'m':1}]))
layers.append(layer('Halo',[{'ty':'el','p':prop([0,0]),'s':prop([114,114])},stroke(purple,5)],scale=anim([(0,[60,60,100]),(22,[108,108,100]),(36,[100,100,100]),(90,[100,100,100])])))
import math
for i in range(8):
 a=i*math.pi/4
 start=[120+math.cos(a)*58,120+math.sin(a)*58,0];end=[120+math.cos(a)*90,120+math.sin(a)*90,0]
 layers.append(layer('Spark '+str(i),[{'ty':'el','p':prop([0,0]),'s':prop([5,5])},{'ty':'fl','c':prop(mint if i%2 else purple),'o':prop(100)}],pos=anim([(0,start),(18,start),(50,end),(90,end)]),scale=anim([(0,[0,0,100]),(18,[0,0,100]),(28,[100,100,100]),(64,[0,0,100]),(90,[0,0,100])])))
save('task_success.json')
layers=[]
layers.append(layer('Paper',[{'ty':'rc','p':prop([0,-10]),'s':prop([72,94]),'r':prop(12)},stroke(purple,5),path([[-19,-27],[17,-27]]),stroke(mint,5),path([[-19,-8],[8,-8]]),stroke(purple,5)],pos=anim([(0,[120,113,0]),(45,[120,101,0]),(90,[120,113,0])])))
layers.append(layer('Tray',[path([[-66,16],[-51,47],[51,47],[66,16]]),stroke(purple,5)],pos=prop([120,143,0])))
layers.append(layer('Quiet sparkle',[path([[0,-9],[0,9]]),path([[-9,0],[9,0]]),stroke(mint,4)],pos=prop([185,65,0]),scale=anim([(0,[60,60,100]),(45,[100,100,100]),(90,[60,60,100])])))
save('empty_list.json')
(out/'README.md').write_text('Original NEW LOOK vector animations authored for this application. No external images, fonts, or network requests. task_success: 1.5 seconds, once; empty_list: 1.5 seconds, limited repetitions.\n')
# Three reusable local onboarding scenes, same visual grammar as the app.
for scene in range(3):
 layers=[]
 if scene==0:
  for i in range(3):
   y=70+i*52
   layers.append(layer('Task card '+str(i),[{'ty':'rc','p':prop([0,0]),'s':prop([146,40]),'r':prop(10)},stroke(purple,4),path([[-48,-1],[-41,6],[-30,-6]]),stroke(mint,4),path([[-13,0],[48,0]]),stroke(purple,4)],pos=anim([(0,[120,y+8,0]),(35+i*8,[120,y,0]),(90,[120,y+8,0])])))
 elif scene==1:
  layers.append(layer('Calendar',[{'ty':'rc','p':prop([0,0]),'s':prop([146,144]),'r':prop(18)},stroke(purple,5),path([[-73,-37],[73,-37]]),stroke(purple,5),path([[-38,-82],[-38,-61]]),path([[38,-82],[38,-61]]),stroke(mint,6)]))
  for i in range(6):
   x=-38+(i%3)*38;y=-8+(i//3)*38
   layers.append(layer('Date '+str(i),[{'ty':'el','p':prop([x,y]),'s':prop([12,12])},{'ty':'fl','c':prop(mint if i==4 else purple),'o':prop(100)}],scale=anim([(0,[90,90,100]),(45,[105,105,100]),(90,[90,90,100])])))
 else:
  for i in range(3):
   layers.append(layer('Reusable sheet '+str(i),[{'ty':'rc','p':prop([0,0]),'s':prop([100,122]),'r':prop(14)},{'ty':'fl','c':prop([.13+i*.035,.15+i*.025,.22+i*.035,1]),'o':prop(100)},stroke(purple,4),path([[-26,-20],[25,-20]]),stroke(mint,5),path([[-26,4],[15,4]]),stroke(purple,5)],pos=anim([(0,[120+(1-i)*19,120+(1-i)*15,0]),(45,[120+(1-i)*27,112+(1-i)*20,0]),(90,[120+(1-i)*19,120+(1-i)*15,0])])))
 save('onboarding_'+str(scene)+'.json')
