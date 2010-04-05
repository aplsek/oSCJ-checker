all: texdoc 

texdoc:	repository.tex
	latex repository; bibtex repository; latex repository; latex repository; dvips repository.dvi -o

figures: 
	ps2epsi ps_graphics/Primitive.ps; ps2epsi ps_graphics/Array.ps; ps2epsi ps_graphics/Scalar.ps; ps2epsi ps_graphics/typehierarchy.ps; ps2epsi ps_graphics/Compound.ps; ps2epsi ps_graphics/TypeName.ps; ps2epsi ps_graphics/Nested.ps; ps2epsi ps_graphics/modehierarchy.ps; ps2epsi ps_graphics/descriptor_hierarchy.ps; ps2epsi ps_graphics/member_hierarchy.ps; ps2epsi ps_graphics/repository_comp.ps

clean:	
	rm *.aux; rm *.ps; rm *.dvi; rm *.log; rm *.bbl; rm *.blg; rm *.toc
