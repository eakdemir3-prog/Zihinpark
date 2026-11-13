"""English Lingo data -> app/src/main/assets/lingo_en.json (pip: wordfreq english-words better_profanity)."""
import json, random, re, os
from wordfreq import top_n_list, zipf_frequency
from english_words import get_english_words_set
from better_profanity import profanity
profanity.load_censor_words()
EXTRA_BAD = {'nazis','nazi','rapes','raped','rapist','slave','slaves','drunk','drugs','bitch','whore','sexy','porno','penis','vagina','boobs','butts','pussy','dildo','semen','sperm','horny','nudes','naked','kills','killer','murder','suicide','corpse','bombs','guns','rifle','pistol','heroin','opium','cocaine','vodka','beers','booze','tits','hitler','satan','devil','demon','jesus','allah','christ','bible','islam','quran','torah','pagan','abort','fetus','gonna','wanna','gotta','sucks','stupid','idiot','dumbs','moron','retard','loser','bloody','damn','damned','hell','crap','shits','pissed','farts','vomit','poops','turds','queer','dyke','faggot','negro','nigga','gypsy','tramp','racist','terror','hostage','bullet','weapon','sniper','hooker','pimps','lesbo','sexual','erotic','fetish','orgasm','condom','hentai','stoned','junkie','smoker','casino','gamble','betting','lotto','dicks','cocks','balls','screw','screwed','bastard','titty','wiener','hymen','anal','anus','rectum','urine','feces','enema','diarrhea'}
BADSET = set()
def bad(w):
    return w in EXTRA_BAD or w in BADSET or profanity.contains_profanity(w)
DICT = get_english_words_set(['web2', 'gcide'], lower=True)
LOWER = {w for w in get_english_words_set(['web2'], lower=False) if w.islower()}
CAPS = {w.lower() for w in get_english_words_set(['web2'], lower=False) if w[0].isupper()}
ok = re.compile('^[a-z]+$')
top = top_n_list('en', 60000)
valid = set()
for w in top:
    if ok.match(w) and len(w) in (5, 6) and w in DICT and not bad(w): valid.add(w)
for w in DICT:
    if ok.match(w) and len(w) in (5, 6) and zipf_frequency(w, 'en') >= 1.5 and not bad(w): valid.add(w)
def good_answer(w):
    if bad(w): return False
    if w.endswith('s') and not w.endswith(('ss', 'us', 'is')): return False
    if w.endswith(('ed', 'ing', 'est', 'ly')) : return False
    if zipf_frequency(w, 'en') < 3.2: return False
    if w in NAMEISH: return False
    return w in LOWER
NAMEISH = {'allan','yahoo','palmer','belle','racism','racist','fatty','gospel','saint','ethnic','pastor','priest','church','sinner','prayer','breast','gender','toilet','abuse','drunk','widow','cancer','hostel','divorce','prison','arrest','victim','corpse','funeral','coffin','alumni','karma','bosom','booty','bimbo','sissy','nigh','chubby','obese','skinny','ugly','lousy','pagan','bible','islam','jesus','satan','devil','demon','ghetto','thug','gangs','bandit','whisky','liquor','tequila','casino','lottery','bitter','larry','chang','peter','laura','murphy','warner','walker','potter','frank','hardy','grace','flora','baron','chile','carol','sandy','jerry','harry','kelly','terry','barry','rocky','penny','perry','ralph','oscar','homer','teddy','misty','dusty','nancy','molly','polly','bobby','billy','danny','jenny','tommy','willy','johnny','andrew','martin','morgan','miller','turner','parker','hunter','porter','barker','carter','wright','cooper','fisher','harper','spencer','taylor','baker','mason','sherry','ruby','patty','maria','anglo','latin','roman','dutch','swiss','india','china','japan','italy','paris','texas','dallas','denver','london','berlin','canada','sydney','jordan','sharon','victor','gordon','norman','warren','dennis','gerald','morris','walter','arthur','philip','howard','harold','herald','wesley','stuart','graham','murray','julian','vernon','lauren','marian','francis','julius','felix','hugh','jesse'}
cands = [w for w in top if ok.match(w) and len(w) in (5, 6) and good_answer(w)]
NAMES = {w.lower() for w in top_n_list('en', 60000) if False}
a5 = [w for w in cands if len(w) == 5][:950]
a6 = [w for w in cands if len(w) == 6][:750]
random.seed(7); random.shuffle(a5); random.shuffle(a6)
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'assets', 'lingo_en.json')
up = str.upper
json.dump({'a5': [up(w) for w in a5], 'a6': [up(w) for w in a6], 'valid': sorted(up(w) for w in valid | set(a5) | set(a6))}, open(out, 'w'), separators=(',', ':'))
print(len(a5), len(a6), len(valid)); print(a5[:80]); print(a6[:60])
