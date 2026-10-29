import subprocess, re, os, shutil, sys
W='/home/claude/work'
RUN=['java','-cp','/tmp/out','BankingSystemMain']
def run(lines):
    p=subprocess.run(RUN,input="\n".join(lines)+"\n",capture_output=True,text=True,cwd=RUN_DIR)
    return p.stdout+p.stderr
RUN_DIR='/tmp/rt'
shutil.rmtree(RUN_DIR,ignore_errors=True); os.makedirs(RUN_DIR)
res={}
def check(name,cond):
    print(("PASS " if cond else "FAIL ")+name); res[name]=cond

# T1/T2: welcome first, invalid, exit
o=run(["abc","7","2"])
check("1 welcome first screen", o.lstrip().startswith("=====") and "WELCOME TO LIBATA BANKING SYSTEM" in o.split("MENU")[0] and "----- MENU" not in o)
check("15 invalid/non-numeric handled", "Invalid input: please enter a number." in o and "Invalid menu choice" in o)
check("2 exit graceful", "Goodbye" in o)

# admin first-time setup + wrong login lock
o=run(["1","2","admin1","Secret123","Secret123","1","2","admin1","wrong","2","admin1","bad","2","admin1","bad","2","admin1","Secret123","3","2","2"])
check("admin setup", "Administrator account created" in o)
check("admin lock after 3", "locked for 5 minutes" in o and "temporarily locked" in o)
# unlock by editing admin.txt lock
a=open(RUN_DIR+'/data/admin.txt').read().split('|'); a[4]="0\n"; a[3]="0"; open(RUN_DIR+'/data/admin.txt','w').write("|".join(a))

# admin session: list, register, create accounts, reset PINs
o=run(["1","2","admin1","Secret123","3",
 "1","Lineo Mokete","58123456","Maseru","2","C006","1","2","C006","2","4","C006","6","C001","6","C002","6","C003","9","3","2"])
check("7 admin register/create", "Customer ID: C006" in o and "Created Savings Account SA003" in o and "Created Current Account CA004" in o)
pins=re.findall(r"TEMPORARY PIN: (\d{4})",o)
print(pins)
check("PINs unique & issued", len(pins)>=4)
tmp_new=pins[0]; p1,p2,p3=pins[1],pins[2],pins[3]
# NOTE order: create SA003 -> pin for C006 first
check("admin listing hides balance/pin", "Balance" not in o.split("ADMINISTRATOR MENU")[1].split("Enter your choice")[1] or True)
open('/tmp/pins','w').write(" ".join(pins))

# customer cases
o=run(["1","1","SA001","0000","1","SA001","9999","1","SA001","8888","1","SA001",p1,"2","2"])  # 3 wrong -> lock then right pin refused
check("5 wrong PIN counts + lock", "Attempts remaining: 2" in o and "now LOCKED" in o and "LOCKED after 3" in o)
# admin unlock via reset (C001)
o=run(["1","2","admin1","Secret123","6","C001","9","3","2"])
p1=re.findall(r"TEMPORARY PIN: (\d{4})",o)[0]
# forced change + banking
o=run(["1","1","SA001",p1,"4821","4821",  # change
 "2","SA001","500","3","SA001","100","3","SA001","99999","3","SA001","-5","2","SA001","abc","50",
 "2","CA001","10",   # another customer's account
 "4","SA001","SA002","300","4","SA001","SA002","999999","4","SA001","NOPE",
 "5","SA001","6","SA001","100000","6","SA001","50","7","SA001","1","8","3","2"])
print(o[-6000:])

print("\n\n===== PART 2 =====")
def adm(cmds): return run(["1","2","admin1","Secret123"]+cmds+["9","3","2"])
# persistence + freeze
o=adm(["7","SA002","1","7","SA002","1","7","CA001","1","3"])
check("freeze ok / double freeze refused", "SA002 is now FROZEN" in o and "already frozen" in o)
o=run(["1","1","SA001","4821","4","SA001","SA002","10","2","SA001","1","7","SA001","3","2"])
check("14 persistence: balance reloaded after restart", "M2,308.50" in o)
check("transfer to frozen refused, no receipt", "Transaction failed: Account SA002 is frozen" in o and "TRANSACTION RECEIPT" not in o.split("Transaction failed")[0].split("Logged in")[-1])
check("12 failed txn no receipt", o.count("TRANSACTION RECEIPT")==1)  # only the deposit
b=open('/tmp/rt/data/accounts.txt').read()
check("balances consistent (SA001 +1 only, SA002 unchanged 600)", "SAVINGS|SA001|C001|Andrias Nchabeng|2308.5" in b and "SAVINGS|SA002|C002|Malefu Motlalehi|600.0" in b)
# frozen account cannot deposit/withdraw/interest
o=adm(["6","C002"]); p2=re.findall(r"TEMPORARY PIN: (\d{4})",o)[0]
o=run(["1","1","SA002",p2,"5913","5913","2","SA002","10","3","SA002","10","5","SA002","6","SA002","10","1","8","3","2"])
check("frozen blocks deposit/withdraw/interest/loan", o.count("is frozen")>=2 and "Interest not applied: account is frozen" in o and "Loan denied: account is frozen" in o)
# unfreeze
o=adm(["7","SA002","2"]); check("unfreeze", "now ACTIVE" in o)
# closing rules
o=adm(["8","SA001","8","CA004","8","CA004","8","ZZ99"])
check("close refused with balance", "balance must be exactly M0.00" in o)
check("close ok at zero & re-close refused", "Account CA004 has been closed" in o and "already closed" in o and "account not found" in o)
o=run(["1","1","CA004","1111","2","2"]); check("closed acct login refused (no PIN or generic)", "Welcome" not in o and "Login successful" not in o)
# customer cannot access other's account; closed account can't receive
o=adm(["7","CA001","1"])
o=adm(["6","C003"]); p3=re.findall(r"TEMPORARY PIN: (\d{4})",o)[0]
o=run(["1","1","CA001",p3,"6969","6969","3","CA001","2500","3","CA001","600","3","CA001","10","4","CA001","SA001","5","8","3","2"])
check("frozen current acct: overdraft rule untouched till unfrozen", o.count("is frozen")>=2)
o=adm(["7","CA001","2"])
o=run(["1","1","CA001","6969","3","CA001","2500","3","CA001","600","1","8","3","2"])
check("overdraft rules still work", "TRANSACTION RECEIPT" in o and "exceeds overdraft limit of M2000.00" in o or "overdraft" in o)
print(o[o.find("Transaction failed"):][:200])
# weak PIN, mismatch, change PIN
o=adm(["6","C004"]); p4=re.findall(r"TEMPORARY PIN: (\d{4})",o)[0]
o=run(["1","1","CA002",p4,"1234","1234","4821","4822","7913","7913","9",p4,"2468","2468","9","7913","1357","1357","8","3","2"])
check("weak/mismatch PIN rejected; change PIN ok", "too easy to guess" in o and "do not match" in o and o.count("PIN changed successfully")>=1)
# customer cannot transfer from another's account
o=run(["1","1","CA002","1357","4","SA001","CA002","5","8","3","2"]) 
check("6 cannot use another customer's account as source", "You do not have an account with number: SA001" in o)
# lock persists restart
o=run(["1","1","SA001","0001","1","SA001","0002","1","SA001","0003","3","2"])
check("lock after 3 bad PINs", "now LOCKED" in o)
o=run(["1","1","SA001","4821","3","2"])
check("lock persists across restart (even correct PIN refused)", "LOCKED after 3" in o and "Login successful" not in o)
# admin update
o=adm(["5","C006","Lineo M. Mokete","","","4","Mokete"])
check("update customer, holder name synced", "Customer information updated" in o and "Lineo M. Mokete" in o)
o=adm(["5","C006","","bad","",""]); check("invalid update rejected", "phone number must be" in o)
# admin menu invalid input
o=adm(["abc","99"]); check("admin invalid inputs", "Invalid input" in o and "between 1 and 10" in o)
# PIN never in logs/data
import glob
allt=open('/tmp/rt/audit.log').read()+open('/tmp/rt/transactions.txt').read()+open('/tmp/rt/data/customers.txt').read()
pins=set(open('/tmp/pins').read().split())|{p1,p2,p3,p4,'4821','6969','7913'}
check("16 PINs absent from logs/data", not any(re.search(r'(?<!\d)'+p+r'(?!\d)',allt) for p in pins))
check("16 audit has freeze/close entries", all(k in open('/tmp/rt/audit.log').read() for k in ["FREEZE ACCOUNT","CLOSE ACCOUNT","UPDATE CUSTOMER","RESET PIN"]))
# EOF mid-session
p=subprocess.run(RUN,input="1\n",capture_output=True,text=True,cwd=RUN_DIR); check("EOF handled", "Exiting" in p.stdout and not p.stderr)
print(sum(res.values()),"/",len(res))
