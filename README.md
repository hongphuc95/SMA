# Systèmes multi-agents producteur / consommateur distribués

## Description
Il s'agit de modéliser un groupe d'agents pouvant parvenir à un certain équilibre, chaque agent tentant d'optimiser un niveau de satisfaction interne de la manière suivante :
- Tout agent produit une marchandise et en consomme une autre
- Tout agent peut acquérir la marchandise qu'il consomme auprès d'un producteur, s'il a de quoi payer.
- Tout agent peut vendre la marchandise qu'il produit s'il trouve un acheteur.
- Le rythme de production est constant jusqu'à atteindre un stock maximal. Lorsque cette valeur maximale est atteinte, l'agent cesse temporairement de produire. Le rythme de consommation est également constant.
- Le niveau de satisfaction de l'agent est à 1 (100%) tant qu'il lui reste un stock de la marchandise qu'il consomme. Il décroît exponentiellement lorsque ce stock est épuisé, jusqu'à ce que l'agent ait pu se réapprovisionner (auquel cas la satisfaction remonte à 1). La satisfaction monte également (one shot) lorsque l'agent cesse de travailler.
- Lorsque l'agent n'est pas satisfait et qu'il n'a plus d'argent, il décide de baisser le prix de la marchandise qu'il produit. On prendra 1 pour prix initial de toutes les marchandises.
- Lorsque l'agent est satisfait et qu'il a de l'argent, il décide d'augmenter le prix de cette marchandise produite.

A l'issue d'une simulation, chaque agent affichera son niveau moyen de satisfaction sur la durée de la simulation. Les agents devait interagir, il est indispensable de se mettre d'accord au préalable sur protocole de communication. On pourra par exemple utiliser les types de messages suivants:
- 1. **CFP** produit (Call For Proposal), quand l'agent cherche à acheter un certain produit
- 2. **PROPOSE** quantité prix, quand l'agent répond qu'il vend ce produit
- 3. **ACCEPT_PROPOSAL** quantité prix, quand l'agent répond qu'il vend ce produit
- 4. **REJECT_PROPOSAL**, pour indiquer qu'on a trouvé moins cher ailleurs
- 5. **CONFIRM**, pour indiquer le transfert effectif de marchandise et d'argent

## Manuel d'utilisation
Pour faire fonctionner le programme :
1. Créer des agents de la classe AgentCommercial
2. Mettre en paramètres (dans le GUI) {marchandise à vendre} {marchandise à consommer} book/paper/ink. Comme c'est montré dans la figure suivante :

![Création d'agents](https://github.com/hongphuc95/SMA/blob/master/img/agent_init.png)

Si on ne choisit pas les arguments, le programme va choisir automatiquement entre les troix choix.

## Diagramme et résultat de la solution
**Rythmes de communication**
- Production : 1 s
- Consommation de produit : 1 s
- Lancement d'un achat d'un produit à consommer : 1 s
- Satisfaction et prix de produit à vendre: 1 s

![Communication entre les agents](https://github.com/hongphuc95/SMA/blob/master/img/communication.png)

Dans l'image ci-dessus :
- L'agent one: cherche paper et vend ink
- L'agent two : cherche paper, vend ink
- Agent three : cherche ink vend book