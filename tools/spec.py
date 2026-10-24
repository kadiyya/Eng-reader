def L(title, wpm, book, start, words, boss=False, **kw):
    return {"title": title, "wpm": wpm, "boss": boss,
            "src": dict(book=book, start=start, words=words, **kw)}

WORLDS = [
    {"name": "Fabl Ormanı", "book": "Aesop's Fables", "author": "Aesop", "color": "#4ADE80", "emoji": "🌲",
     "levels": [
        L("The Hare and the Tortoise", 125, "aesop2.txt", "A Hare was one day making fun", 130),
        L("The Lion and the Mouse", 130, "aesop2.txt", "A Lion asleep in his lair", 150),
        L("The Shepherd's Boy and the Wolf", 135, "aesop2.txt", "A Shepherd's Boy was tending", 160),
        L("The North Wind and the Sun", 140, "aesop2.txt", "A dispute arose between the North Wind", 140),
        L("The Town Mouse and the Country Mouse", 150, "aesop2.txt", "A Town Mouse and a Country Mouse", 260, boss=True),
     ]},
    {"name": "Harikalar Diyarı", "book": "Alice's Adventures in Wonderland", "author": "Lewis Carroll", "color": "#60A5FA", "emoji": "🐇",
     "levels": [
        L("Down the Rabbit-Hole", 145, "alice.txt", "Alice was beginning to get very tired", 220, occ=1),
        L("Falling", 150, "alice.txt", "Either the well was very deep", 200),
        L("Drink Me", 155, "alice.txt", "There seemed to be no use in waiting by the little door", 200),
        L("The Cheshire Cat", 160, "alice.txt", "'Would you tell me, please, which way", 170),
        L("A Mad Tea-Party", 170, "alice.txt", "There was a table set out under a tree", 300, boss=True),
     ]},
    {"name": "Noel Şarkısı", "book": "A Christmas Carol", "author": "Charles Dickens", "color": "#F87171", "emoji": "🕯️",
     "levels": [
        L("Marley Was Dead", 150, "carol.txt", "MARLEY was dead: to begin with", 200, fix=[("MARLEY", "Marley")]),
        L("The Tight-Fisted Hand", 155, "carol.txt", "Oh! But he was a tight-fisted hand", 200),
        L("A Merry Christmas, Uncle!", 160, "carol.txt", "\"A merry Christmas, uncle! God save you!", 220),
        L("The Knocker", 170, "carol.txt", "Now, it is a fact, that there was nothing at all particular about the knocker", 220),
        L("Christmas Morning", 180, "carol.txt", "YES! and the bedpost was his own", 300, boss=True, fix=[("YES!", "Yes!"), ("Laocooen", "Laocoon")]),
     ]},
    {"name": "Baker Sokağı", "book": "The Adventures of Sherlock Holmes", "author": "Arthur Conan Doyle", "color": "#A78BFA", "emoji": "🔍",
     "levels": [
        L("The Woman", 160, "holmes.txt", "To Sherlock Holmes she is always", 220),
        L("You See, But You Do Not Observe", 165, "holmes.txt", "I could not help laughing at the ease", 200),
        L("The Red-Headed League", 175, "holmes.txt", "I had called upon my friend, Mr. Sherlock Holmes, one day in the autumn", 220),
        L("The Speckled Band", 180, "holmes.txt", "On glancing over my notes of the seventy odd cases", 230),
        L("The Art of Deduction", 190, "holmes.txt", "\"You must not fear", 300, boss=True),
     ]},
    {"name": "Longbourn Malikânesi", "book": "Pride and Prejudice", "author": "Jane Austen", "color": "#F472B6", "emoji": "🎩",
     "levels": [
        L("A Truth Universally Acknowledged", 165, "pride.txt", "It is a truth universally acknowledged", 230),
        L("Mr. Bingley", 170, "pride.txt", "Mr. Bingley was good-looking and gentlemanlike", 220),
        L("The Ball at Meryton", 180, "pride.txt", "Elizabeth Bennet had been obliged, by the scarcity of gentlemen", 230),
        L("Elizabeth's Walk", 185, "pride.txt", "Elizabeth continued her walk alone, crossing field after field", 220),
        L("In Vain I Have Struggled", 195, "pride.txt", "\"In vain I have struggled", 400, boss=True),
     ]},
    {"name": "Poe'nun Karanlık Şatosu", "book": "Tales and Poems", "author": "Edgar Allan Poe", "color": "#FFB547", "emoji": "🦇",
     "levels": [
        L("The Raven", 170, "poe5.txt", "Once upon a midnight dreary", 200, poem=True),
        L("The Cask of Amontillado", 180, "poe2.txt", "thousand injuries of Fortunato", 230,
          fix=[("thousand injuries", "The thousand injuries")]),
        L("The Masque of the Red Death", 190, "poe2.txt", "\"Red Death\" had long devastated", 240,
          fix=[("\"Red Death\"", "The \"Red Death\"")]),
        L("The House of Usher", 200, "poe2.txt", "DURING the whole of a dull, dark, and soundless day", 250, fix=[("DURING", "During")]),
        L("The Tell-Tale Heart", 210, "poe2.txt", "TRUE!", 350,
          fix=[("TRUE!", "True!")]),
     ]},
]
