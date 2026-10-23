package com.kadiyya.engreader

data class Level(
    val number: Int,
    val title: String,
    val stage: String,
    val wpm: Int,
    val text: String,
) {
    val wordCount: Int get() = countWords(text)
}

fun countWords(text: String): Int =
    text.split(Regex("\\s+")).count { it.isNotBlank() }

object Levels {
    val all: List<Level> = listOf(
        Level(
            1, "My Morning", "Başlangıç · A2", 100,
            """
            I wake up at seven o'clock every morning. First, I open the window and take a deep breath. Then I go to the kitchen and make a cup of tea. I like my tea hot and sweet. While the water is boiling, I wash my face and brush my teeth. For breakfast, I usually eat bread, cheese, olives and a boiled egg. I eat slowly and look at my phone for the news. After breakfast, I get dressed and check the weather. If it is sunny, I walk to work. If it is rainy, I take the bus. I love quiet mornings because they help me start the day with a calm mind.
            """.trimIndent()
        ),
        Level(
            2, "A Walk in the Park", "Başlangıç · A2", 110,
            """
            On Sunday afternoon, I went for a walk in the park near my house. The weather was warm and the sky was clear. Many families were sitting on the grass and having a picnic. Children were running, laughing and playing with a big red ball. An old man was feeding the birds next to the lake. I sat on a bench under a tall tree and listened to the sounds around me. A small dog came to me and wagged its tail, so I said hello to it. After an hour, I bought an ice cream and walked back home. It was a simple day, but I felt happy and relaxed.
            """.trimIndent()
        ),
        Level(
            3, "At the Coffee Shop", "Başlangıç · A2", 120,
            """
            Good morning! Can I have a large latte, please? Yes, with oat milk, thank you. Could you also add one shot of vanilla? Great. And I'd like a chocolate muffin as well. No, I'll have it here, not to go. How much is that? Okay, can I pay by card? Perfect. By the way, do you have Wi-Fi here? Oh, what's the password? Thanks a lot. Is it okay if I sit by the window? It looks really nice today. Sorry, could I get a glass of water too? Thank you so much. Have a lovely day! See you tomorrow, probably at the same time.
            """.trimIndent()
        ),
        Level(
            4, "My Weekend Plans", "Temel · B1", 130,
            """
            This weekend is going to be really busy, but I'm looking forward to it. On Saturday morning, I'm meeting my best friend for breakfast. We haven't seen each other for almost two months, so we have a lot to talk about. After that, I'm planning to go shopping because I need a new pair of running shoes. In the evening, my cousins are coming over for dinner, and I'm going to cook pasta with a homemade tomato sauce. On Sunday, I want to sleep a little longer and then go for a long run by the sea. If I have enough energy, I'll finally finish the book I started last month.
            """.trimIndent()
        ),
        Level(
            5, "Introducing Myself at Work", "Temel · B1", 135,
            """
            Hi everyone, it's really nice to meet you all. My name is Alex, and I've just joined the team as a junior software developer. Before this, I worked for a small startup for about two years, where I mostly built mobile apps. I studied computer engineering at university, and I've always been interested in how technology can make people's lives easier. Outside of work, I enjoy hiking, playing the guitar and trying new restaurants around the city. I'm really excited to be here, and I'm sure I'm going to learn a lot from you. Please don't hesitate to reach out if you need anything. I'm looking forward to working together.
            """.trimIndent()
        ),
        Level(
            6, "A Trip to the Seaside", "Temel · B1", 140,
            """
            Last summer, my family and I decided to spend a week in a small town by the sea. We drove for almost six hours, and by the time we arrived, everyone was tired but excited. Our hotel was only a few steps from the beach, so we could hear the waves from our room. Every morning, we swam in the clear blue water before it got too hot. In the afternoons, we explored the narrow streets of the old town and tried fresh seafood at local restaurants. One evening, we took a boat trip and watched the sunset from the water. It was one of the most beautiful things I've ever seen, and I still think about it.
            """.trimIndent()
        ),
        Level(
            7, "Learning Something New", "Orta · B1+", 150,
            """
            A few years ago, I realized that I had stopped learning new things. My days were all the same: work, dinner, television and sleep. So I made a decision to learn one new skill every year. The first year, I learned how to cook properly, and it completely changed the way I eat. The second year, I started playing the piano, which was much harder than I expected. There were many moments when I wanted to give up, especially when my fingers simply wouldn't do what my brain was telling them. But I kept practicing for twenty minutes a day, and slowly, the music started to sound like music. The most important lesson I learned is that progress is quiet. You don't notice it day by day, but one morning you look back and realize how far you've come.
            """.trimIndent()
        ),
        Level(
            8, "Talking About a Movie", "Orta · B1+", 155,
            """
            Have you seen the new science fiction movie everyone's talking about? I watched it last night, and honestly, I'm still thinking about it. The story takes place in the near future, when people can upload their memories to a computer. The main character is a scientist who discovers that someone has been changing her memories without her permission. At first, the movie feels slow, but after the first thirty minutes, it becomes really intense. The acting is fantastic, and the music fits every scene perfectly. What I liked most was that it didn't give easy answers. It made me ask myself how much of who I am depends on what I remember. If you enjoy movies that make you think, I definitely recommend it, but don't watch it when you're tired.
            """.trimIndent()
        ),
        Level(
            9, "Giving Directions", "Orta · B1+", 160,
            """
            Excuse me, are you looking for the central station? No problem, it's not far from here, maybe ten minutes on foot. First, go straight along this street until you reach the big traffic lights. Then turn left and walk past the pharmacy and the bakery on the corner. You'll see a small square with a fountain in the middle. Cross the square and take the second street on your right. Keep going for about two hundred meters, and the station will be right in front of you. If you get lost, just ask someone, because everyone around here knows it. Actually, if you're in a hurry, you could also take the number twelve bus from that stop over there. It only takes three minutes, but it can be crowded at this time of day. Good luck, and have a safe trip!
            """.trimIndent()
        ),
        Level(
            10, "The Job Interview", "Orta-Üst · B2", 170,
            """
            Thank you for inviting me today. I'd like to start by saying that I've followed your company's work for quite a while, and I really admire the way you combine creativity with practical solutions. In my previous role, I was responsible for managing a team of five people and coordinating projects with clients from several different countries. One of my biggest achievements was reducing our delivery time by almost thirty percent, mainly by improving the way we communicated and shared information. Of course, it wasn't always easy. There were times when deadlines were tight and opinions were divided, but I learned that listening carefully is often more powerful than talking. I believe my experience, together with my curiosity and willingness to learn, would allow me to make a real contribution to your team from the very first day.
            """.trimIndent()
        ),
        Level(
            11, "Technology and Daily Life", "Orta-Üst · B2", 175,
            """
            It's hard to imagine our daily lives without technology. We wake up to an alarm on our phones, check our messages before we even get out of bed, and use maps to find our way through cities we've lived in for years. In many ways, technology has made life more convenient. We can talk to friends on the other side of the world, learn almost anything online and work from wherever we want. However, these benefits come with a cost. Many people find it increasingly difficult to focus on a single task for a long period of time, because notifications constantly interrupt them. Others feel that they're always available and never truly able to switch off. The challenge, then, isn't to reject technology, but to use it intentionally, so that it serves our goals instead of controlling our attention.
            """.trimIndent()
        ),
        Level(
            12, "Telling a Story", "Orta-Üst · B2", 180,
            """
            So, let me tell you what happened to me last Friday, because it was honestly one of the strangest days of my life. I was running late for an important meeting, and of course, my train was delayed by twenty minutes. When I finally got to the office, I realized I'd left my laptop at home, with the entire presentation on it. I was about to panic, but then I remembered that I'd sent a copy to a colleague the night before. I called her, she printed everything out, and we managed to start the meeting only five minutes late. Just as I was beginning to relax, the fire alarm went off, and we all had to leave the building. We ended up finishing the presentation outside, standing in the parking lot, and believe it or not, the client loved it. They said it was the most memorable meeting they'd ever had.
            """.trimIndent()
        ),
        Level(
            13, "Presenting an Idea", "İleri · C1", 190,
            """
            Good afternoon, everyone. Today I'd like to propose a relatively simple change that, in my view, could significantly improve the way we work together. At the moment, most of our decisions are made in long meetings that involve far more people than necessary. As a result, we spend hours discussing issues that could be resolved in a few minutes, and the people who actually need to focus on their work are constantly pulled away from it. What I'm suggesting is that we replace the majority of these meetings with short written updates, shared at the beginning of each week. Anyone who has questions or concerns can respond in writing, and we'll only schedule a meeting when a genuine discussion is needed. Based on a small pilot we ran last quarter, this approach saved each team member roughly four hours a week, while the quality of our decisions remained exactly the same, or even improved.
            """.trimIndent()
        ),
        Level(
            14, "Debating Remote Work", "İleri · C1", 200,
            """
            The debate about remote work has become surprisingly emotional, and I think that's because both sides are partly right. Supporters argue that working from home eliminates long commutes, gives people more control over their time and allows companies to hire talented employees regardless of where they live. These are real and significant advantages. On the other hand, critics point out that some things are genuinely harder to achieve at a distance. Spontaneous conversations, the kind that often lead to unexpected ideas, rarely happen on a video call. New employees, in particular, may struggle to learn the unwritten rules of a company when they never see their colleagues in person. Ultimately, I'd argue that the question isn't whether remote work is good or bad, but which kinds of work benefit from being together, and which kinds benefit from being apart. The most successful organizations will be those that answer that question honestly, rather than following whatever happens to be fashionable.
            """.trimIndent()
        ),
        Level(
            15, "The Art of Small Talk", "İleri · C1", 210,
            """
            Many people dismiss small talk as superficial, a polite but meaningless exchange about the weather or the traffic. Yet if you look a little closer, you'll find that it plays a surprisingly important role in how we build relationships. Small talk is essentially a low-risk way of signaling that we're friendly, approachable and willing to connect. It gives both people a chance to find common ground before moving on to more personal or serious topics. The secret to doing it well isn't having clever things to say, but showing genuine curiosity. Instead of asking questions that can be answered with a simple yes or no, try asking open questions, such as what someone enjoyed most about their weekend or how they got into their line of work. Then, and this is the crucial part, actually listen to the answer and build on it. Before long, what started as a casual comment about the rain can turn into a conversation you both remember.
            """.trimIndent()
        ),
    )
}
