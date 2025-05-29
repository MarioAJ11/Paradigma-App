## Instrucciones para importar esta wiki a tu repositorio

1. Antes de nada, debes conocer que la wiki de un repositorio es tratado como un repositorio más, y por tanto, las acciones que podemos hacer son las mismas que para cualquier repositorio. _También soporta ramas, pero solo apareceran en la wiki los commits de la rama por defecto_
2. Clona la wiki en local
   
   1. por ssh
   
   `git clone git@github.com:iesgrancapitan-proyectos/Plantilla_Documentacion_Wiki_PI.wiki.git `
   
   1. por https

    `git clone https://github.com/iesgrancapitan-proyectos/Plantilla_Documentacion_Wiki_PI.wiki.git `
   
3. En tu repositorio de Proyecto Integrado ya creado, ve al apartado **wiki** y crea tu primera página **"create the first page"**.
4. Crea la página con los valores por defecto "Save Page"
5. Una vez creada tu primera página, se genera su correspondiente .git. Puedes verlo a la derecha en "clone this wiki local"
6. Copialo. Recuerda que si usas ssh tienes que sustituir el "https://github.com/" del principio de la url por "git@github.com:"
7. Añade a la wiki plantilla clonada en local el remote generado en la wiki de tu proyecto copiado arriba
   
   `git remote add wiki https://github.com/iesgrancapitan-proyectos/NOMBRE_DE_TU_REPOSITORIO.wiki.git `
   
8. Haz un push forzando la subida
   
   `git push wiki main -f`
   
9.  Actualiza el apartado wiki de tu repositorio. Ya deberías tener la plantilla de la wiki en tu repositorio.